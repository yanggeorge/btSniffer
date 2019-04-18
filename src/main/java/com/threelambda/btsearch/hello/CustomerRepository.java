package com.threelambda.btsearch.hello;

/**
 * Created by ym on 2019-04-19
 */
import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface CustomerRepository extends CrudRepository<Customer, Long> {

    List<Customer> findByLastName(String lastName);
}
