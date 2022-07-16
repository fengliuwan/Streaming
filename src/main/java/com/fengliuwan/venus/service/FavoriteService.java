package com.fengliuwan.venus.service;

import com.fengliuwan.venus.dao.FavoriteDao;
import com.fengliuwan.venus.entity.db.Item;
import com.fengliuwan.venus.entity.db.ItemType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FavoriteService {

    @Autowired
    private FavoriteDao favoriteDao;

    /**
     * insert item as user's favorite item
     * @param userId
     * @param item
     */
    public void setFavoriteItem(String userId, Item item) {
        favoriteDao.setFavoriteItem(userId, item);
    }

    public void unsetFavoriteItem(String userId, String itemId) {
        favoriteDao.unsetFavoriteItems(userId, itemId);
    }

    /**
     * return a map of favorite items, mapping itemtype to a list of items of this type
     * @param userId
     * @return
     */
    public Map<String, List<Item>> getFavoriteItems(String userId) {
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), new ArrayList<>()); // need to convert item type to string
        }
        Set<Item> favorites = favoriteDao.getFavoriteItems(userId);
        for(Item item : favorites) {
            itemMap.get(item.getType().toString()).add(item); // need to convert type to string
        }
        return itemMap;
    }
}
