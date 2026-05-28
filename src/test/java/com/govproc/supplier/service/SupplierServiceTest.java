package com.govproc.supplier.service;

import com.govproc.shared.exception.BusinessException;
import com.govproc.supplier.domain.Supplier;
import com.govproc.supplier.dto.CreateSupplierRequest;
import com.govproc.supplier.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock SupplierRepository supplierRepository;
    @InjectMocks SupplierService supplierService;

    @Test
    void createSupplier_deveRetornarResponse_quandoDadosValidos() {
        var request = new CreateSupplierRequest("Tech Ltda", "Tech", "12345678000195",
                "tech@email.com", "11999999999", "TI");
        when(supplierRepository.existsByDocument("12345678000195")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        var response = supplierService.createSupplier(request);

        assertThat(response.companyName()).isEqualTo("Tech Ltda");
        assertThat(response.document()).isEqualTo("12345678000195");
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplier_deveLancarBusiness_quandoDocumentoJaCadastrado() {
        var request = new CreateSupplierRequest("Tech Ltda", null, "12345678000195", null, null, null);
        when(supplierRepository.existsByDocument("12345678000195")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.createSupplier(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("12345678000195");
    }
}
