package com.corporationxyz.mapper;


import com.corporationxyz.dto.ClientLimitConfigDto;
import com.corporationxyz.dto.ClientLimitDto;
import com.corporationxyz.persistence.entity.ClientLimitConfig;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface ClientLimitConfigMapper {
    ClientLimitConfigDto toDto(ClientLimitConfig entity);

    ClientLimitConfig toEntity(ClientLimitDto dto);
}
