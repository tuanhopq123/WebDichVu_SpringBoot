package com.example.WebDichVu_SpringBoot.dto;

import lombok.Data;

@Data
public class CassoPayload {

  private int error;

  private CassoTransaction data;
}