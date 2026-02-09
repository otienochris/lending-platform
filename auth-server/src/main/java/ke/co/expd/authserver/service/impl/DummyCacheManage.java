package ke.co.expd.authserver.service.impl;

import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

@Service
public class DummyCacheManage {
    public Cache getCache(String users) {
        return null;
    }
}
