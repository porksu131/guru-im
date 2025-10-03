package com.guru.im.demo.gui;

import com.guru.im.common.model.ResponseResult;
import com.guru.im.demo.gui.component.RoundedPasswordField;
import com.guru.im.demo.gui.component.RoundedTextField;
import com.guru.im.demo.model.UserInfo;
import com.guru.im.demo.service.ApiService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LoginFrame extends JFrame {
    private JComboBox<String> userComboBox;
    private RoundedTextField usernameField;
    private RoundedPasswordField passwordField;
    private List<UserInfo> userList = new ArrayList<>();

    public LoginFrame() {
        initUsers();
        setupUI();
    }

    public LoginFrame(String environment) {
        ApiService.init(environment);
        initUsers();
        setupUI();
    }

    private void initUsers() {
        ResponseResult<List<UserInfo>> listResponseResult = ApiService.queryAllUser();
        if (ResponseResult.isSuccess(listResponseResult) && CollectionUtils.isNotEmpty(listResponseResult.getData())) {
            userList.clear();
            userList.addAll(listResponseResult.getData());
        }
    }

    private void setupUI() {
        URL imageURL = LoginFrame.class.getClassLoader().getResource("image/chat-logo.png");
        if (imageURL != null) {
            setIconImage(new ImageIcon(imageURL).getImage());
        }
        setTitle("聊天客户端登录");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(new Color(240, 242, 245));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 10, 10, 5);
        gbc.fill = GridBagConstraints.BOTH;


        // 用户下拉列表
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        JLabel selectUserLabel = new JLabel("选择用户:");
        selectUserLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        selectUserLabel.setPreferredSize(new Dimension(60, 32));
        mainPanel.add(selectUserLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 0;
        userComboBox = new JComboBox<>(userList.stream().map(UserInfo::getUserName).toArray(String[]::new));
        userComboBox.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        userComboBox.setPreferredSize(new Dimension(180, 32));
        userComboBox.addActionListener(e -> updateFields());
        userComboBox.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        mainPanel.add(userComboBox, gbc);

        // 用户名
        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel userNameLabel = new JLabel("用户名:");
        userNameLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        mainPanel.add(userNameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        usernameField = new RoundedTextField();
        usernameField.setPlaceholder("请输入用户姓名");
        usernameField.setPreferredSize(new Dimension(180, 32));
        mainPanel.add(usernameField, gbc);

        // 密码
        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("密码:");
        passwordLabel.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        mainPanel.add(passwordLabel, gbc);
        gbc.gridx = 1;
        gbc.gridy = 2;

        passwordField = new RoundedPasswordField();
        passwordField.setPlaceholder("请输入用户密码");
        passwordField.setPreferredSize(new Dimension(180, 32));
        mainPanel.add(passwordField, gbc);
        // 按钮
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 20);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.setBackground(null);

        JButton loginBtn = new JButton("登录");
        loginBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        loginBtn.setBackground(new Color(0, 150, 136));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> login());

        JButton registerBtn = new JButton("注册");
        registerBtn.setBackground(new Color(255, 99, 71));
        registerBtn.addActionListener(e -> register());
        registerBtn.setFont(new Font("Microsoft YaHei", Font.PLAIN, 14));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(loginBtn);
        buttonPanel.add(registerBtn);

        mainPanel.add(buttonPanel, gbc);

        updateFields();
        add(mainPanel);
    }

    private void updateFields() {
        int index = userComboBox.getSelectedIndex();
        if (index >= 0 && index < userList.size()) {
            UserInfo user = userList.get(index);
            usernameField.setText(user.getUserName());
            if (StringUtils.isBlank(user.getPassword())) {
                passwordField.setText("123456"); // 测试期间，懒得输入
            } else {
                passwordField.setText(user.getPassword());
            }
        }
    }

    private void login() {
        try {
            String password = new String(passwordField.getText());
            String userName = usernameField.getText();
            if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
                JOptionPane.showMessageDialog(LoginFrame.this, "用户名或密码为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ResponseResult<UserInfo> result = ApiService.login(userName, password);
            if (ResponseResult.isSuccess(result)) {
                new MainFrame(result.getData()).setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(LoginFrame.this, "登录失败:" + result.getMsg(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(LoginFrame.this, "用户ID必须是数字", "错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(LoginFrame.this, "登录异常：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void register() {
        try {
            String password = new String(passwordField.getText());
            String userName = usernameField.getText();
            if (StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
                JOptionPane.showMessageDialog(LoginFrame.this, "用户名或密码为空", "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            ResponseResult<UserInfo> result = ApiService.register(userName, password);
            if (ResponseResult.isSuccess(result)) {
                JOptionPane.showMessageDialog(LoginFrame.this, "注册成功:" + result.getMsg(), "成功", JOptionPane.INFORMATION_MESSAGE);
                this.initUsers();
                userComboBox.setModel(new DefaultComboBoxModel<>(userList.stream().map(UserInfo::getUserName).toArray(String[]::new)));
                userComboBox.setSelectedItem(result.getData().getUserName());
            } else {
                JOptionPane.showMessageDialog(LoginFrame.this, "注册失败:" + result.getMsg(), "错误", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(LoginFrame.this, "用户ID必须是数字", "错误", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(LoginFrame.this, "注册异常：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
}



