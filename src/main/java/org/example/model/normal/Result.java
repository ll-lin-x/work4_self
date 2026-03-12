package org.example.model.normal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result {
    private Base base;
    private Object data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Base{
        private int code;
        private String msg;
    }

    public static Result success(Object data) {
        Result result = new Result();
        Base  base = new Base();
        base.setCode(1);
        base.setMsg("success");
        result.setData(data);
        result.setBase(base);
        return result;
    }

    public static Result success() {
        Result result = new Result();
        Base  base = new Base();
        base.setCode(1);
        base.setMsg("success");
        result.setBase(base);
        return result;
    }

    public static Result error(String message) {
        Result result = new Result();
        Base  base = new Base();
        base.setCode(-1);
        base.setMsg(message);
        result.setBase(base);
        return result;
    }
}
