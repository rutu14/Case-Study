package com.casestudy.ApiGateway.dto;

public class UserDTO {
	
	private String id;
	
	private String login;
	
	private String token;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getLogin() {
		return login;
	}

	public void setLogin(String login) {
		this.login = login;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
	
	public UserDTO(String id, String login, String token) {
		super();
		this.id = id;
		this.login = login;
		this.token = token;
	}

	public UserDTO() {	}

}


