package com.example.mely.service;

import com.example.mely.dto.request.CreateEmployeeRequest;
import com.example.mely.dto.response.CreateEmployeeResponse;
import com.example.mely.model.Enums.RoleType;
import com.example.mely.model.User;
import com.example.mely.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class EmployeeService {

    private final UserRepository userRepository;
    public final PasswordEncoder passwordEncoder;

    public EmployeeService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    )
    {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CreateEmployeeResponse createEmployee(CreateEmployeeRequest request, String adminEmail){
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(()-> new RuntimeException("Admin not found"));

        if (admin.getRoleType()!= RoleType.ADMIN){
            throw new RuntimeException("Admin role not allowed");
        }

        if (userRepository.existsByEmail(request.getEmail())){
            throw new RuntimeException("User already exists");
        }

        String password = generateTemporaryPassword();

        User employee = new User();
        employee.setEmail(request.getEmail());
        employee.setPassword(password);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setRoleType(request.getRoleType());
        employee.setMustChangePassword(true);
        employee.setBankId(admin.getBankId());

        userRepository.save(employee);

        return new CreateEmployeeResponse(
          employee.getId(),
          employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getRoleType(),
                employee.getPassword()
        );
    }

    private String generateTemporaryPassword(){
        return UUID.randomUUID().toString().substring(0,10);
    }
}
