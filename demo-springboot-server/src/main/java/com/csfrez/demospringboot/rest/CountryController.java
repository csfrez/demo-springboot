package com.csfrez.demospringboot.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.csfrez.demospringboot.model.Country;
import com.csfrez.demospringboot.service.CountryService;

@Controller
@RequestMapping("/country")
public class CountryController {

    @Autowired
    private CountryService countryService;
    /*
    @RequestMapping
    public ModelAndView getAll(Country country) {
        ModelAndView result = new ModelAndView("index");
        List<Country> countryList = countryService.getAllByWeekend(country);
        result.addObject("pageInfo", new PageInfo<Country>(countryList));
        result.addObject("queryParam", country);
        result.addObject("page", country.getPage());
        result.addObject("rows", country.getRows());
        return result;
    }
    */

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