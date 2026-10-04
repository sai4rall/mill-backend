package com.millsystem.service;

import com.millsystem.dto.ExportOrderDto;
import com.millsystem.entity.Customer;
import com.millsystem.entity.ExportOrder;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.CustomerRepository;
import com.millsystem.repository.ExportOrderRepository;
import com.millsystem.repository.ExportShipmentRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExportOrderService {

    private final ExportOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final MillRepository millRepository;
    private final ExportShipmentRepository shipmentRepository;

    public ExportOrderService(ExportOrderRepository orderRepository, CustomerRepository customerRepository,
                               MillRepository millRepository, ExportShipmentRepository shipmentRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.millRepository = millRepository;
        this.shipmentRepository = shipmentRepository;
    }

    public List<ExportOrderDto> getAll(Long millId) {
        return orderRepository.findByMillIdOrderByOrderDateDesc(millId).stream().map(o -> {
            BigDecimal shipped = shipmentRepository.sumExportedByOrderId(o.getId());
            return ExportOrderDto.from(o, shipped);
        }).collect(Collectors.toList());
    }

    public ExportOrderDto getById(Long id, Long millId) {
        ExportOrder order = orderRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        BigDecimal shipped = shipmentRepository.sumExportedByOrderId(id);
        return ExportOrderDto.from(order, shipped);
    }

    @Transactional
    public ExportOrderDto create(ExportOrderDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        Customer customer = customerRepository.findByIdAndMillId(dto.getCustomerId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + dto.getCustomerId()));

        ExportOrder order = new ExportOrder();
        order.setMill(mill); order.setCustomer(customer); order.setOrderDate(dto.getOrderDate());
        order.setRiceType(dto.getRiceType()); order.setQuantityQuintals(dto.getQuantityQuintals());
        order.setPricePerQuintal(dto.getPricePerQuintal());
        if (dto.getPricePerQuintal() != null) {
            order.setTotalAmount(dto.getQuantityQuintals().multiply(dto.getPricePerQuintal()));
        }
        order.setNotes(dto.getNotes());
        order = orderRepository.save(order);
        return ExportOrderDto.from(order, BigDecimal.ZERO);
    }

    @Transactional
    public ExportOrderDto updateStatus(Long id, ExportOrder.OrderStatus status, Long millId) {
        ExportOrder order = orderRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.setStatus(status);
        order = orderRepository.save(order);
        BigDecimal shipped = shipmentRepository.sumExportedByOrderId(id);
        return ExportOrderDto.from(order, shipped);
    }
}
