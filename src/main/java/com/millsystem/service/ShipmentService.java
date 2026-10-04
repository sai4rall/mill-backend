package com.millsystem.service;

import com.millsystem.dto.ShipmentDto;
import com.millsystem.entity.ExportOrder;
import com.millsystem.entity.ExportShipment;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.ExportOrderRepository;
import com.millsystem.repository.ExportShipmentRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private final ExportShipmentRepository shipmentRepository;
    private final ExportOrderRepository orderRepository;
    private final MillRepository millRepository;

    public ShipmentService(ExportShipmentRepository shipmentRepository, ExportOrderRepository orderRepository,
                           MillRepository millRepository) {
        this.shipmentRepository = shipmentRepository;
        this.orderRepository = orderRepository;
        this.millRepository = millRepository;
    }

    public List<ShipmentDto> getByOrder(Long orderId, Long millId) {
        return shipmentRepository.findByOrderIdAndMillIdOrderByShipmentDateDesc(orderId, millId)
                .stream().map(ShipmentDto::from).collect(Collectors.toList());
    }

    @Transactional
    public ShipmentDto create(ShipmentDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        ExportOrder order = orderRepository.findByIdAndMillId(dto.getOrderId(), millId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + dto.getOrderId()));

        ExportShipment shipment = new ExportShipment();
        shipment.setMill(mill); shipment.setOrder(order); shipment.setShipmentDate(dto.getShipmentDate());
        shipment.setQuintalsExported(dto.getQuintalsExported());
        shipment.setVehicleNumber(dto.getVehicleNumber()); shipment.setNotes(dto.getNotes());
        shipmentRepository.save(shipment);

        BigDecimal totalShipped = shipmentRepository.sumExportedByOrderId(order.getId());
        order.setStatus(totalShipped.compareTo(order.getQuantityQuintals()) >= 0
                ? ExportOrder.OrderStatus.FULFILLED : ExportOrder.OrderStatus.PARTIAL);
        orderRepository.save(order);
        return ShipmentDto.from(shipment);
    }
}
