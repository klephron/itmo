package ru.itmo.zavar.accountservice.service;

import lombok.AllArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.accountservice.dto.BillDto;
import ru.itmo.zavar.accountservice.entity.ApartmentEntity;
import ru.itmo.zavar.accountservice.entity.BillEntity;
import ru.itmo.zavar.accountservice.exception.ObjectNotFoundException;
import ru.itmo.zavar.accountservice.mapper.BillMapper;
import ru.itmo.zavar.accountservice.repository.BillRepository;

import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@AllArgsConstructor
@Service
public class BillService {
    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final MongoTemplate mongoTemplate;

    public BillDto getBillById(String id) throws ObjectNotFoundException {
        BillEntity bill = this.billRepository.findById(id).orElseThrow(() -> new ObjectNotFoundException("Bill not found"));
        return this.billMapper.toDto(bill);
    }

    public List<BillDto> getAllBills() {
        List<BillEntity> bills = this.billRepository.findAll();
        return bills.stream().map(this.billMapper::toDto).toList();
    }

    public List<BillDto> getAllBillsByUserId(String userId) {
        List<BillEntity> bills = this.billRepository.findByUserId(userId);
        return bills.stream().map(this.billMapper::toDto).toList();
    }

    public BillDto createBill(String userId, Long heat, Long cost) {
        BillEntity bill = new BillEntity();
        bill.setUserId(userId);
        bill.setHeat(heat);
        bill.setCost(cost);
        bill.setPayed(false);
        bill.setTimestamp(System.currentTimeMillis());
        BillEntity save = this.billRepository.save(bill);
        return this.billMapper.toDto(save);
    }

    @Transactional
    public List<BillDto> generateBills(long fromTs, long toTs, long tariff) {
        Aggregation aggregation = newAggregation(
                match(Criteria.where("timestamp").gte(fromTs).lt(toTs)),
                sort(Sort.Direction.DESC, "timestamp"),
                group("device_id").first("heat").as("latest_heat"),
                lookup("radiator_to_apartment", "_id", "deviceId", "apartment_mapping"),
                unwind("apartment_mapping"),
                group("apartment_mapping.apartmentId").sum("latest_heat").as("total_heat")
        );

        AggregationResults<Document> results =
                mongoTemplate.aggregate(aggregation, "radiator", Document.class);

        List<BillDto> createdBills = new ArrayList<>();

        for (Document doc : results) {
            String apartmentId = (String) doc.get("_id");
            Long heat = doc.getLong("total_heat");
            Long cost = heat * tariff;

            ApartmentEntity apartment = mongoTemplate.findById(apartmentId, ApartmentEntity.class);
            if (apartment == null || apartment.getUserId() == null) continue;

            BillDto bill = this.createBill(apartment.getUserId(), heat, cost);
            createdBills.add(bill);
        }

        return createdBills;
    }

    public void notifyUsers(List<BillDto> bills) {

    }


}
