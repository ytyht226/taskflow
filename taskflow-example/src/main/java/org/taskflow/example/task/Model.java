package org.taskflow.example.task;

import lombok.Data;

/**
 * Created by ytyht226 on 2023/3/17.
 */
@Data
public class Model {
    private int id;
    private String name;

    public Model(int id) {
        this.id = id;
    }
    public Model(int id, String name) {
        this.id = id;
        this.name = name;
    }
}