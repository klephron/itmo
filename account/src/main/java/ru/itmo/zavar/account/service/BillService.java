package ru.itmo.zavar.account.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.zavar.account.dto.BillDto;
import ru.itmo.zavar.account.dto.UserDto;
import ru.itmo.zavar.account.entity.ApartmentEntity;
import ru.itmo.zavar.account.entity.BillEntity;
import ru.itmo.zavar.account.exception.ObjectNotFoundException;
import ru.itmo.zavar.account.mapper.BillMapper;
import ru.itmo.zavar.account.repository.BillRepository;
import ru.itmo.zavar.account.repository.UserRepository;

import java.util.*;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Slf4j(topic = "AS-BillService")
@AllArgsConstructor
@Service
public class BillService {
    private final BillRepository billRepository;
    private final BillMapper billMapper;
    private final MongoTemplate mongoTemplate;
    private final UserService userService;
    private final CacheManager cacheManager;
    private final JavaMailSender mailSender;

    public BillDto getBillById(String id) throws ObjectNotFoundException {
        BillEntity bill = this.billRepository.findById(id).orElseThrow(() -> {
            log.info("Bill not found for id {}", id);
            return new ObjectNotFoundException("Bill not found");
        });
        log.info("Bill found for id {}", bill.getId());
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
        log.info("Bill created {}", save);
        return this.billMapper.toDto(save);
    }

    //@Cacheable(value = "bills", key = "'bills:' + #fromTs + ':' + #toTs + ':' + #tariff")
    @Transactional
    public List<BillDto> generateBills(long fromTs, long toTs, long tariff, boolean useCache) {
        String cacheKey = String.format("bills:%d:%d:%d", fromTs, toTs, tariff);
        Cache cache = cacheManager.getCache("bills");

        if (useCache) {
            List<BillDto> cached = cache.get(cacheKey, List.class);
            if (cached != null) {
                log.info("Returning cached bills for key {}", cacheKey);
                return cached;
            }
        }

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
            log.info("Bill generated {}", bill);
            createdBills.add(bill);
        }

        if (useCache) {
            cache.put(cacheKey, createdBills);
            log.info("Put bills to cache with key {}", cacheKey);
        }

        return createdBills;
    }

    @Async
    public void notifyUsers(List<BillDto> bills) {
        bills.forEach(billDto -> {
            UserDto userDto = userService.loadUserById(billDto.getUserId());
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(userDto.getEmail());
            message.setSubject("Bill " + billDto.getId());
            message.setText("You got a bill, please pay it: " + billDto.getCost() + " BTC");
            message.setFrom("ahla@noreply.ru");
            mailSender.send(message);
            log.info("User {} notifyed", userDto.getId());
        });
    }


}
