package com.example.demo.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.demo.dto.front.DealDto;
import com.example.demo.entity.User;
import com.example.demo.service.DealService;
import com.example.demo.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class DealController {

    @Autowired
    private DealService dealService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private com.example.demo.service.WishlistService wishlistService;

    @GetMapping("/deals")
    public String dealsPage(HttpSession session, Model model) {
        Map<String, String> user = (Map<String, String>) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        
        // ダークモード設定を取得（デフォルトはtrue）
        Long userId = Long.parseLong(user.get("id"));
        User fullUser = userService.findById(userId);
        boolean darkMode = fullUser == null || !fullUser.isDarkMode();
        model.addAttribute("username", user.get("name"));
        model.addAttribute("userid", user.get("id"));
        model.addAttribute("darkMode", darkMode);
        
        return "deals";
    }

    @GetMapping("/api/deals")
    @ResponseBody
    public List<DealDto> getDeals(
            @RequestParam(defaultValue = "steam") String stores,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "48") int limit,
            @RequestParam(defaultValue = "") String sort
    ) {
        List<String> selectedStores = Arrays.asList(stores.split(","));
        return dealService.fetchDeals(selectedStores, offset, limit, sort);
    }

    @GetMapping("/api/deal/lookup")
    @ResponseBody
    public DealDto getDealLookup(@RequestParam String id, 
                                 @RequestParam(required = false) String shop,
                                 @RequestParam(required = false, defaultValue = "false") boolean update,
                                 HttpSession session) {
        DealDto dto = dealService.getDealWithOthers(id);
        
        if (update && shop != null && dto != null) {
            Map<String, String> user = (Map<String, String>) session.getAttribute("user");
            if (user != null) {
                Long userId = Long.parseLong(user.get("id"));
                
                // 特定ショップの情報を探す
                DealDto targetDeal = null;
                
                // メインが一致するか
                if (shop.equalsIgnoreCase(dto.getShop())) {
                    targetDeal = dto;
                } else if (dto.getOtherDeals() != null) {
                    // 他のストアから探す
                    for (DealDto.OtherDealInfo other : dto.getOtherDeals()) {
                        if (shop.equalsIgnoreCase(other.getShop())) {
                            // 簡易DTO作成
                            targetDeal = new DealDto();
                            targetDeal.setGameID(dto.getGameID()); // IDは同じ
                            // otherからコピー
                            targetDeal.setShop(other.getShop());
                            targetDeal.setPriceNew(other.getPriceNew());
                            targetDeal.setPriceOld(other.getPriceOld());
                            targetDeal.setCut(other.getCut());
                            targetDeal.setExpiry(other.getExpiry());
                            targetDeal.setUrl(other.getUrl());
                            
                            // 履歴情報はメインから継承 (StoreLowは不明になるが、他は共通)
                            targetDeal.setHistoryLow(dto.getHistoryLow());
                            targetDeal.setHistoryLow1y(dto.getHistoryLow1y());
                            targetDeal.setHistoryLow3m(dto.getHistoryLow3m());
                            break;
                        }
                    }
                }
                
                if (targetDeal != null) {
                    wishlistService.updateFromDto(userId, id, shop, targetDeal);
                }
            }
        }
        
        return dto;
    }
}