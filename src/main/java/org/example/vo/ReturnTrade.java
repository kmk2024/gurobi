package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReturnTrade {

    String id;
    long qty;
    double fee;
    double divRate;

}
