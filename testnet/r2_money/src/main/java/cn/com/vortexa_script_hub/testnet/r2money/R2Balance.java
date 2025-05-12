package cn.com.vortexa_script_hub.testnet.r2money;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * @author helei
 * @since 2025-04-26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class R2Balance {
    private String account;
    private String walletAddress;
    private BigDecimal USDC;
    private BigDecimal R2USD;
    private BigDecimal SR2USD;
    private BigInteger gasFee;
}
