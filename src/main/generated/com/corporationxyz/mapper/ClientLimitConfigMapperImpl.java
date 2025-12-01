package com.corporationxyz.mapper;

import com.corporationxyz.dto.ClientLimitConfigDto;
import com.corporationxyz.dto.ClientLimitDto;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-01T15:28:15+0200",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (Oracle Corporation)"
)
@Component
public class ClientLimitConfigMapperImpl implements ClientLimitConfigMapper {

    @Override
    public ClientLimitConfigDto toDto(ClientLimitConfig entity) {
        if ( entity == null ) {
            return null;
        }

        ClientLimitConfigDto clientLimitConfigDto = new ClientLimitConfigDto();

        clientLimitConfigDto.setClientId( entity.getClientId() );
        if ( entity.getMonthlyLimit() != null ) {
            clientLimitConfigDto.setMonthlyLimit( entity.getMonthlyLimit() );
        }
        if ( entity.getWindowCapacity() != null ) {
            clientLimitConfigDto.setWindowCapacity( entity.getWindowCapacity() );
        }
        if ( entity.getWindowDurationSeconds() != null ) {
            clientLimitConfigDto.setWindowDurationSeconds( entity.getWindowDurationSeconds() );
        }

        return clientLimitConfigDto;
    }

    @Override
    public ClientLimitConfig toEntity(ClientLimitDto dto) {
        if ( dto == null ) {
            return null;
        }

        ClientLimitConfig clientLimitConfig = new ClientLimitConfig();

        clientLimitConfig.setClientId( dto.getClientId() );
        clientLimitConfig.setMonthlyLimit( dto.getMonthlyLimit() );
        clientLimitConfig.setWindowCapacity( dto.getWindowCapacity() );
        clientLimitConfig.setWindowDurationSeconds( dto.getWindowDurationSeconds() );

        return clientLimitConfig;
    }
}
