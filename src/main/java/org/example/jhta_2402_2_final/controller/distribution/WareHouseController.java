package org.example.jhta_2402_2_final.controller.distribution;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.jhta_2402_2_final.model.dto.distribution.CombineLogDto;
import org.example.jhta_2402_2_final.model.dto.distribution.KitOrderDetailLogDto;
import org.example.jhta_2402_2_final.model.dto.distribution.LogisticsWareHouseDto;
import org.example.jhta_2402_2_final.model.dto.distribution.ProductOrderLogDto;
import org.example.jhta_2402_2_final.service.distribution.LogisticsWareHouseService;
//import org.example.jhta_2402_2_final.util.SmsUtil;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/wareHouse")
public class WareHouseController {

    private final LogisticsWareHouseService logisticsWareHouseService;

    //    private final SmsUtil smsUtil;
    @GetMapping("/selectAll")
    public String selectAll(Model model) {

        // 프러덕트 오더 테이블에서 스테이터스가 5인 경우 창고에 적재하고 5 -> 7로 바꿈
        int result = logisticsWareHouseService.insertWarehouseStackForCompletedOrders();
        if (result > 0) {
            List<String> productOrderIdList = logisticsWareHouseService.selectProductOrderIdByStatus(5);
            logisticsWareHouseService.updateProductOrderStatus();
            logisticsWareHouseService.insertProductOrderLog(productOrderIdList);

        }

        // 밀키트 오더 테이블에서 스테이터스가 3인 경우 창고에서 차감하고 6->8으로 바꿈
        List<Map<String, Object>> requiredStackList = logisticsWareHouseService.selectRequiredStack();
        if (!requiredStackList.isEmpty()) {
            for (Map<String, Object> list : requiredStackList) {
                String sourceFk = (String) list.get("sourceFk");
                BigDecimal totalQuantity = (BigDecimal) list.get("totalQuantity");
                int quantity = totalQuantity.intValue();
                Map<String, Object> params = new HashMap<>();
                params.put("sourceFk", sourceFk);
                params.put("quantity", quantity);

                logisticsWareHouseService.updateStackFirstRecord(params);
            }
            List<String> kitOrderIdList = logisticsWareHouseService.selectKitOrderIdByStatus(6);
            logisticsWareHouseService.updateKitOrderStatus();
            if (!kitOrderIdList.isEmpty()) {
                int resultLog=logisticsWareHouseService.insertKitOrderLog(kitOrderIdList);
                if (resultLog > 0) {
                    System.out.println("성공적으로 KIT_ORDER_LOG가 들어갔습니다.");
                }
            }else System.out.println("selectKitOrderIdStatus에서 불러온 값이 없습니다.");


        }


        logisticsWareHouseService.deleteZeroQuantityRecords();

        List<LogisticsWareHouseDto> list = logisticsWareHouseService.selectAllLogisticsWarehouse();

        model.addAttribute("warehouseList", list);

        return "distribution/wareHouseList";
    }

    @GetMapping("/selectBySourceName")
    public String selectBySourceName(@RequestParam String keyword, Model model) {


        List<LogisticsWareHouseDto> list = logisticsWareHouseService.selectBySourceNameLogisticsWarehouse(keyword);

        model.addAttribute("warehouseList", list);
        return "distribution/wareHouseList";
    }


    @GetMapping("/selectLog")
    @ResponseBody
    public List<CombineLogDto> getWarehouseLog(@RequestParam String sourceId) {

        List<KitOrderDetailLogDto> kitOrderLogs = logisticsWareHouseService.selectKitOrderLogDetailsBySourceId(sourceId);
        List<ProductOrderLogDto> productOrderLogs = logisticsWareHouseService.selectProductOrderLogDetailsBySourceId(sourceId);


        List<CombineLogDto> combineLogs = new ArrayList<>();
        for (KitOrderDetailLogDto kitLog : kitOrderLogs) {
            combineLogs.add(new CombineLogDto(
                    kitLog.getKitOrderId(),
                    kitLog.getCompanyName(),
                    kitLog.getSourceName(),
                    kitLog.getSourceId(),
                    kitLog.getQuantity(),
                    kitLog.getOrderDate(),
                    kitLog.getStatus()
            ));
        }
        for (ProductOrderLogDto productLog : productOrderLogs) {
            combineLogs.add(new CombineLogDto(
                    productLog.getProductOrderId(),
                    productLog.getCompanyName(),
                    productLog.getSourceName(),
                    productLog.getSourceId(),
                    productLog.getQuantity(),
                    productLog.getOrderDate(),
                    productLog.getStatus()
            ));
        }

        Collections.sort(combineLogs, new Comparator<CombineLogDto>() {
            @Override
            public int compare(CombineLogDto o1, CombineLogDto o2) {
                return o2.getOrderDate().compareTo(o1.getOrderDate());
            }
        });

        return combineLogs;

    }
//    @GetMapping("/sendMSG")
//    @ResponseBody
//    public void sendMSG() {
//        smsUtil.sendOne();
//    }


}
