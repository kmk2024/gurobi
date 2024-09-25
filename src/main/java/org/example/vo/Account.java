package org.example.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Account {
    String name;
    double balance;

    double cost;
}
