package org.example.service;

import org.example.model.dto.UserLoginDTO;
import org.example.model.pojo.User;

public interface LoginService {
    User login(UserLoginDTO user);

}
