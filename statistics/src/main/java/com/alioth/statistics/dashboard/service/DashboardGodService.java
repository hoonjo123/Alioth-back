package com.alioth.statistics.dashboard.service;

import com.alioth.statistics.dashboard.dto.res.DashboardBestTeamResDto;
import com.alioth.statistics.dashboard.dto.res.DashboardGodResDto;
import com.alioth.statistics.domain.contract.domain.Contract;
import com.alioth.statistics.domain.contract.repository.ContractRepository;
import com.alioth.statistics.domain.dummy.domain.ContractStatus;
import com.alioth.statistics.domain.member.domain.SalesMembers;
import com.alioth.statistics.domain.team.domain.Team;
import com.alioth.statistics.domain.team.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardGodService {

    private final ContractRepository contractRepository;
    private final TeamRepository teamRepository;


    public DashboardGodResDto getSalesGod() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth yearMonth = YearMonth.from(now);
        LocalDateTime startTime = LocalDateTime.of(now.getYear(), yearMonth.getMonth(), 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(now.getYear(), yearMonth.getMonth(), yearMonth.atEndOfMonth().getDayOfMonth(), 23, 59, 59);

        List<Contract> all = contractRepository.findByCreatedAtBetween(startTime, endTime);

        Map<SalesMembers, List<Contract>> collectList = all.stream()
                .collect(Collectors.groupingBy(Contract::getSalesMembers));

        List<DashboardGodResDto> dtoList = new ArrayList<>();

        for (var entry : collectList.entrySet()) {
            SalesMembers member = entry.getKey();
            List<Contract> contracts = entry.getValue();

            BigDecimal sumPrice = contracts.stream()
                    .filter(x -> x.getSalesMembers().getPerformanceReview().equals("A"))
                    .map(x -> new BigDecimal(x.getContractTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            long sumCount = contracts.stream()
                    .filter(x -> x.getSalesMembers().getPerformanceReview().equals("A"))
                    .mapToLong(Contract::getContractCount)
                    .sum();

            DashboardGodResDto temp = DashboardGodResDto.builder()
                    .name(member.getName())
                    .price(sumPrice.toEngineeringString())
                    .count(String.valueOf(sumCount))
                    .build();

            dtoList.add(temp);
        }

        dtoList.sort((o1, o2) -> {
            BigDecimal o1Price = new BigDecimal(o1.price());
            BigDecimal o2Price = new BigDecimal(o2.price());

            return o2Price.compareTo(o1Price);
        });

        return dtoList.get(0);
    }



    public DashboardBestTeamResDto getBestTeam() {
        LocalDateTime now = LocalDateTime.now();
        YearMonth yearMonth = YearMonth.from(now);
        LocalDateTime startTime = LocalDateTime.of(now.getYear(), yearMonth.getMonth(), 1, 0, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(now.getYear(), yearMonth.getMonth(), yearMonth.atEndOfMonth().getDayOfMonth(), 23, 59, 59);

        List<Team> aTeam = teamRepository.findAll()
                .stream()
                .filter(x -> x.getPerformanceReview().equals("A"))
                .toList();

        List<SalesMembers> list = aTeam.get(0).getTeamMembers();

        BigDecimal totalPrice = BigDecimal.ZERO;
        Long totalCount = 0L;
        for (var members : list) {
            List<Contract> contracts = contractRepository.findBySalesMembersAndCreatedAtBetween(members, startTime, endTime);

            BigDecimal sumPrice = contracts
                    .stream()
                    .filter(x->x.getContractStatus() != ContractStatus.Cancellation)
                    .map(x -> new BigDecimal(x.getContractTotalPrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Long sumCount = contracts
                    .stream()
                    .filter(x->x.getContractStatus() != ContractStatus.Cancellation)
                    .count();

            totalPrice = totalPrice.add(sumPrice);
            totalCount = totalCount + sumCount;
        }

        return DashboardBestTeamResDto.builder()
                .teamName(aTeam.get(0).getTeamName())
                .price(totalPrice.toEngineeringString())
                .count(String.valueOf(totalCount))
                .build();
    }


}
