package bupt.database.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private String message;
    private Integer code;
    private T data;

    public static <T> R<T> success(T data){
        return new R<>("ok", 200, data);
    }

    public static <T> R<T> fail(String message){
        return new R<>(message, 400, null);
    }
}
