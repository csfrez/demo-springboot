package com.csfrez.demospringboot.service;

import java.util.List;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.csfrez.demospringboot.mapper.CountryMapper;
import com.csfrez.demospringboot.model.Country;
import tk.mybatis.mapper.entity.Example;
import tk.mybatis.mapper.weekend.Weekend;
import tk.mybatis.mapper.weekend.WeekendCriteria;

@Service
public class CountryService {

    @Autowired
    private CountryMapper countryMapper;
    
    public List<Country> getAll(Country country, int pageNum, int pageSize) {
        if (pageNum <= 0) {
            pageNum = 1; 
        }
        if (pageSize <= 0) {
        	pageSize = 10;
        }
        PageHelper.startPage(pageNum, pageSize);
        Example example = new Example(Country.class);
        Example.Criteria criteria = example.createCriteria();
        if (country.getCountryname() != null && country.getCountryname().length() > 0) {
            criteria.andLike("countryname", "%" + country.getCountryname() + "%");
        }
        if (country.getCountrycode() != null && country.getCountrycode().length() > 0) {
            criteria.andLike("countrycode", "%" + country.getCountrycode() + "%");
        }
        return countryMapper.selectByExample(example);
    }

    public List<Country> getAllByWeekend(Country country, int pageNum, int pageSize) {
    	if (pageNum <= 0) {
            pageNum = 1; 
        }
        if (pageSize <= 0) {
        	pageSize = 10;
        }
        PageHelper.startPage(pageNum, pageSize);
        Weekend<Country> weekend = Weekend.of(Country.class);
        WeekendCriteria<Country, Object> criteria = weekend.weekendCriteria();
        if (country.getCountryname() != null && country.getCountryname().length() > 0) {
            criteria.andLike(Country::getCountryname, "%" + country.getCountryname() + "%");
        }
        if (country.getCountrycode() != null && country.getCountrycode().length() > 0) {
            criteria.andLike(Country::getCountrycode, "%" + country.getCountrycode() + "%");
        }
        return countryMapper.selectByExample(weekend);
    }
    
    public Country getById(Integer id) {
        return countryMapper.selectByPrimaryKey(id);
    }

    public void deleteById(Integer id) {
        countryMapper.deleteByPrimaryKey(id);
    }

    public void save(Country country) {
        if (country.getId() != null) {
            countryMapper.updateByPrimaryKey(country);
        } else {
            countryMapper.insert(country);
        }
    }
}