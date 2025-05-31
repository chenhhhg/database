package bupt.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String username;
    private Integer userId;
    private Integer role;
    private Long expiresIn; // token过期时间（毫秒）
} 