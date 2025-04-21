package bupt.database.util;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class R<T> {
    private Integer code;
    private T data;
    private String msg;

    public static <T> R<T> success(T data) {
        return new R<>(200, data, "success");
    }

    public static <T> R<T> error(String msg) {
        return new R<>(500, null, msg);
    }


}
