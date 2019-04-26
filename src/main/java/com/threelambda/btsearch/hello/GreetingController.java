package com.threelambda.btsearch.hello;

/**
 * Created by ym on 2019-04-19
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class GreetingController {

    private Logger logger = LoggerFactory.getLogger(GreetingController.class);

    @Autowired
    private CustomerRepository customerRepository;

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name, Model model) {
        model.addAttribute("name", name);
        test();
        return "greeting";
    }

    private void test() {
        Iterable<Customer> all = customerRepository.findAll();
        for (Customer customer : all) {
            logger.debug(customer.toString());
        }
    }

}