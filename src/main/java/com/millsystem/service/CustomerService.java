package com.millsystem.service;

import com.millsystem.dto.CustomerDto;
import com.millsystem.entity.Customer;
import com.millsystem.entity.Mill;
import com.millsystem.exception.ResourceNotFoundException;
import com.millsystem.repository.CustomerRepository;
import com.millsystem.repository.MillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final MillRepository millRepository;

    public CustomerService(CustomerRepository customerRepository, MillRepository millRepository) {
        this.customerRepository = customerRepository;
        this.millRepository = millRepository;
    }

    public List<CustomerDto> getAll(Long millId) {
        return customerRepository.findByMillIdOrderByNameAsc(millId)
                .stream().map(CustomerDto::from).collect(Collectors.toList());
    }

    public CustomerDto getById(Long id, Long millId) {
        return customerRepository.findByIdAndMillId(id, millId)
                .map(CustomerDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    @Transactional
    public CustomerDto create(CustomerDto dto, Long millId) {
        Mill mill = millRepository.findById(millId)
                .orElseThrow(() -> new ResourceNotFoundException("Mill not found"));
        Customer customer = new Customer();
        customer.setMill(mill);
        applyFields(customer, dto);
        return CustomerDto.from(customerRepository.save(customer));
    }

    @Transactional
    public CustomerDto update(Long id, CustomerDto dto, Long millId) {
        Customer customer = customerRepository.findByIdAndMillId(id, millId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
        applyFields(customer, dto);
        return CustomerDto.from(customerRepository.save(customer));
    }

    private void applyFields(Customer c, CustomerDto dto) {
        c.setName(dto.getName()); c.setPhone(dto.getPhone()); c.setEmail(dto.getEmail());
        c.setAddress(dto.getAddress()); c.setCompanyName(dto.getCompanyName());
    }
}
