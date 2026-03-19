package org.example.model.domin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.example.model.pojo.User;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginUserCache {
    private User user;
    private List<String> permissions;
}
