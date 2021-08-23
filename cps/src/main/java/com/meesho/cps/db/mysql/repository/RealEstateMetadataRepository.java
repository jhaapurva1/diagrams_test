package com.meesho.cps.db.mysql.repository;

import com.meesho.commons.enums.Country;
import com.meesho.cps.data.entity.mysql.RealEstateMetadata;

import org.springframework.data.repository.CrudRepository;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * @author shubham.aggarwal
 * 03/08/21
 */
public interface RealEstateMetadataRepository extends CrudRepository<RealEstateMetadata, Integer> {

    List<RealEstateMetadata> findAllByUpdatedAtAfterAndCountryOrderByUpdatedAtAsc(ZonedDateTime updatedAt,
                                                                                  Country country);

    List<RealEstateMetadata> findAllByCountryOrderByUpdatedAtAsc(Country country);

}
