package com.znsio.teswiz.businessLayer.cryptoAPI;

import com.znsio.teswiz.api.TeswizApiResponse;
import com.znsio.teswiz.runner.Runner;
import com.znsio.teswiz.services.ApiService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CryptoAPIBL {
    private static final Logger LOGGER = LogManager.getLogger(CryptoAPIBL.class.getName());
    private final Map<String, Object> testData = Runner.getTestDataAsMap("Crypto_API");
    private final String base_URL = testData.get("url").toString();

    public TeswizApiResponse getDataUsingCryptoSymbol(String symbol) {
        LOGGER.info("Getting crypto currency data for last 24-Hrs");
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("symbol", symbol);
        TeswizApiResponse response = ApiService.get(base_URL, queryParams);
        assertThat(response.getStatusCode()).as("API status code incorrect!")
                .isEqualTo(200);
        return response;
    }

    public CryptoAPIBL verifypriceChange(TeswizApiResponse response, int maxPriceChange) {
        LOGGER.info("Verifying price change is less than " + maxPriceChange);
        double priceChange = response.asJsonObject().getDouble("priceChange");
        assertThat(priceChange)
                .as("Price change value more than expected maximum value!")
                .isLessThan(maxPriceChange);
        return this;
    }

    public CryptoAPIBL verifyPriceChangePercent(TeswizApiResponse response, int maxPriceChangePercent) {
        LOGGER.info("Verifying price change percent is less than " + maxPriceChangePercent);
        double priceChangePercent = response.asJsonObject().getDouble("priceChangePercent");
        assertThat(priceChangePercent)
                .as("Price change percent value more than expected maximum value!")
                .isLessThan(maxPriceChangePercent);
        return this;
    }
}
