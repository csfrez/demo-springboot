package com.csfrez.demospringboot.rest;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.csfrez.demospringboot.model.Country;
import com.csfrez.demospringboot.service.CountryService;

@RestController
@RequestMapping("/country")
public class CountryController {

    @Autowired
    private CountryService countryService;
    
    @RequestMapping(value = "/getall")
    public  List<Country> getAll(Country country, int pageNum, int pageSize) {
        List<Country> countryList = countryService.getAll(country, pageNum, pageSize);
        return countryList;
    }
    
    @RequestMapping(value = "/getallbyweekend")
    public  List<Country> getAllByWeekend(Country country, int pageNum, int pageSize) {
        List<Country> countryList = countryService.getAllByWeekend(country, pageNum, pageSize);
        return countryList;
    }

    @RequestMapping(value = "/view/{id}")
    @ResponseBody
    public Country view(@PathVariable Integer id) {
        Country country = countryService.getById(id);
        return country;
    }

    @RequestMapping(value = "/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        countryService.deleteById(id);
        return "delete success " + id;
    }

    @RequestMapping(value = "/save")
    @ResponseBody
    public String save(Country country) {
        countryService.save(country);
        return "success " + country.getId();
    }
}