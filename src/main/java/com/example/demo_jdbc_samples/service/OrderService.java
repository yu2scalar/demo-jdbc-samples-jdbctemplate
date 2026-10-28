package com.example.demo_jdbc_samples.service;

import com.example.demo_jdbc_samples.dto.OrderDetailDto;
import com.example.demo_jdbc_samples.dto.OrderDetailLineDto;
import com.example.demo_jdbc_samples.dto.OrderLineDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderRequestDto;
import com.example.demo_jdbc_samples.dto.PlaceOrderResponseDto;
import com.example.demo_jdbc_samples.dto.ResponseStatusDto;
import com.example.demo_jdbc_samples.exception.CustomException;
import com.example.demo_jdbc_samples.model.CustomerCustomers;
import com.example.demo_jdbc_samples.model.InventoryItems;
import com.example.demo_jdbc_samples.model.OrderingOrders;
import com.example.demo_jdbc_samples.model.OrderingStatements;
import com.example.demo_jdbc_samples.repository.CustomerCustomersRepository;
import com.example.demo_jdbc_samples.repository.InventoryItemsRepository;
import com.example.demo_jdbc_samples.repository.OrderingOrdersRepository;
import com.example.demo_jdbc_samples.repository.OrderingStatementsRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Service;

/**
 * Business logic of scalardb-sample (PlaceOrder / GetOrder / GetOrders) plus
 * CancelOrder, with JDBC. Same steps as the ScalarDB version's OrderService.
 *
 * Each method is one transaction (TransactionRunner, READ COMMITTED, with retry).
 * Rows that are read, checked and written back (items, customer, the cancelled order)
 * are read with SELECT ... FOR UPDATE, so that concurrent updates are not lost: with
 * plain reads in READ COMMITTED, two transactions could both read the same stock and
 * both write their own result. ScalarDB detects that conflict at commit and fails one
 * of them; here the second one waits for the first. Items are locked in item_id order
 * and before the customer, in PlaceOrder and CancelOrder, to avoid deadlocks.
 */
@Service
public class OrderService {

    // Transaction attempts including retries; the load test reads the difference
    private final AtomicLong attemptCount = new AtomicLong();

    private final TransactionRunner transactionRunner;
    private final CustomerCustomersRepository customersRepository;
    private final InventoryItemsRepository itemsRepository;
    private final OrderingOrdersRepository ordersRepository;
    private final OrderingStatementsRepository statementsRepository;

    public OrderService(TransactionRunner transactionRunner,
            CustomerCustomersRepository customersRepository, InventoryItemsRepository itemsRepository,
            OrderingOrdersRepository ordersRepository, OrderingStatementsRepository statementsRepository) {
        this.transactionRunner = transactionRunner;
        this.customersRepository = customersRepository;
        this.itemsRepository = itemsRepository;
        this.ordersRepository = ordersRepository;
        this.statementsRepository = statementsRepository;
    }

    // PlaceOrder: reduce stock, charge the customer, write the order and its statements
    public PlaceOrderResponseDto placeOrder(PlaceOrderRequestDto request) throws CustomException {
        OrderRules.validate(request);
        return transactionRunner.run(() -> {
            String orderId = UUID.randomUUID().toString();
            List<OrderLineDto> lines = request.getItems();
            OrderingStatements[] statements = new OrderingStatements[lines.size()];
            long totalAmount = 0;
            for (int i : OrderRules.indexesByItemId(lines)) {
                OrderLineDto line = lines.get(i);
                InventoryItems item = itemsRepository.getInventoryItemsForUpdate(
                    InventoryItems.builder().itemId(line.getItemId()).build());
                if (item.getStock() < line.getQty()) {
                    throw new CustomException("Out of stock: item_id=" + line.getItemId()
                        + ", stock=" + item.getStock() + ", qty=" + line.getQty(), OrderRules.OUT_OF_STOCK);
                }
                item.setStock(item.getStock() - line.getQty());
                itemsRepository.updateInventoryItems(item);
                totalAmount += item.getPrice() * line.getQty();
                statements[i] = OrderingStatements.builder()
                    .orderId(orderId)
                    .lineNo(i + 1)
                    .itemId(line.getItemId())
                    .qty(line.getQty())
                    .unitPrice(item.getPrice())
                    .build();
            }

            CustomerCustomers customer = customersRepository.getCustomerCustomersForUpdate(
                CustomerCustomers.builder().customerId(request.getCustomerId()).build());
            if (customer.getCreditTotal() + totalAmount > customer.getCreditLimit()) {
                throw new CustomException("Credit limit exceeded: customer_id=" + request.getCustomerId()
                    + ", credit_total=" + customer.getCreditTotal() + ", amount=" + totalAmount
                    + ", credit_limit=" + customer.getCreditLimit(), OrderRules.CREDIT_LIMIT_EXCEEDED);
            }
            customer.setCreditTotal(customer.getCreditTotal() + totalAmount);
            customersRepository.updateCustomerCustomers(customer);

            ordersRepository.insertOrderingOrders(OrderingOrders.builder()
                .orderId(orderId)
                .customerId(request.getCustomerId())
                .createdAt(System.currentTimeMillis())
                .status(OrderRules.STATUS_ORDERED)
                .totalAmount(totalAmount)
                .build());
            for (OrderingStatements statement : statements) {
                statementsRepository.insertOrderingStatements(statement);
            }
            return PlaceOrderResponseDto.builder().orderId(orderId).totalAmount(totalAmount).build();
        }, attemptCount);
    }

    // GetOrder: the order, its statements and the item names
    public OrderDetailDto getOrder(String orderId) throws CustomException {
        return transactionRunner.run(() -> {
            OrderingOrders order = ordersRepository.getOrderingOrders(
                OrderingOrders.builder().orderId(orderId).build());
            return buildOrderDetail(order);
        }, attemptCount);
    }

    // GetOrders: all orders of a customer, newest first.
    // method = "index" or "scan": accepted for the same API as the ScalarDB version;
    // with JDBC both are the same query (WHERE customer_id = ?, which uses the index).
    public List<OrderDetailDto> getOrdersByCustomerId(int customerId, String method) throws CustomException {
        if (!"index".equals(method) && !"scan".equals(method)) {
            throw new CustomException("method must be index or scan: " + method, OrderRules.INVALID_REQUEST);
        }
        return transactionRunner.run(() -> {
            List<OrderingOrders> orders = ordersRepository.getOrderingOrdersListByCustomerId(customerId);
            orders.sort(Comparator.comparing(OrderingOrders::getCreatedAt).reversed());
            List<OrderDetailDto> details = new ArrayList<>();
            for (OrderingOrders order : orders) {
                details.add(buildOrderDetail(order));
            }
            return details;
        }, attemptCount);
    }

    // CancelOrder: give back the stock and the credit, then mark the order CANCELLED.
    // The statements are kept. The order row is locked first, so that two cancels of
    // the same order run one after the other and the second one gets 9802.
    public ResponseStatusDto cancelOrder(String orderId) throws CustomException {
        return transactionRunner.run(() -> {
            OrderingOrders order = ordersRepository.getOrderingOrdersForUpdate(
                OrderingOrders.builder().orderId(orderId).build());
            OrderRules.checkCancellable(orderId, order.getStatus());

            List<OrderingStatements> statements = statementsRepository.getOrderingStatementsListByPk(
                OrderingStatements.builder().orderId(orderId).build());
            statements.sort(Comparator.comparing(OrderingStatements::getItemId));
            for (OrderingStatements statement : statements) {
                InventoryItems item = itemsRepository.getInventoryItemsForUpdate(
                    InventoryItems.builder().itemId(statement.getItemId()).build());
                item.setStock(item.getStock() + statement.getQty());
                itemsRepository.updateInventoryItems(item);
            }

            CustomerCustomers customer = customersRepository.getCustomerCustomersForUpdate(
                CustomerCustomers.builder().customerId(order.getCustomerId()).build());
            customer.setCreditTotal(customer.getCreditTotal() - order.getTotalAmount());
            customersRepository.updateCustomerCustomers(customer);

            order.setStatus(OrderRules.STATUS_CANCELLED);
            ordersRepository.updateOrderingOrders(order);
            return ResponseStatusDto.builder().code(0).message("").build();
        }, attemptCount);
    }

    private OrderDetailDto buildOrderDetail(OrderingOrders order) {
        List<OrderingStatements> statements = statementsRepository.getOrderingStatementsListByPk(
            OrderingStatements.builder().orderId(order.getOrderId()).build());
        List<OrderDetailLineDto> lines = new ArrayList<>();
        for (OrderingStatements statement : statements) {
            InventoryItems item = itemsRepository.getInventoryItems(
                InventoryItems.builder().itemId(statement.getItemId()).build());
            lines.add(OrderDetailLineDto.builder()
                .lineNo(statement.getLineNo())
                .itemId(statement.getItemId())
                .itemName(item.getName())
                .qty(statement.getQty())
                .unitPrice(statement.getUnitPrice())
                .subtotal(statement.getUnitPrice() * statement.getQty())
                .build());
        }
        return OrderDetailDto.builder()
            .orderId(order.getOrderId())
            .customerId(order.getCustomerId())
            .createdAt(order.getCreatedAt())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .items(lines)
            .build();
    }

    public long getAttemptCount() {
        return attemptCount.get();
    }
}
