/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export enum NotificationsControllerStatus {
  READ = "READ",
  UNREAD = "UNREAD",
  ALL = "ALL",
}

export enum CaseEntityStatus {
  OPENED = "OPENED",
  FROZEN = "FROZEN",
  CLOSED = "CLOSED",
  REJECTED = "REJECTED",
}

export interface AddCaseRequest {
  /** @minLength 1 */
  title: string;
  description: string;
  users: Record<string, string>;
}

export interface AddUpdateIncidentRequest {
  applicant: string;
  /** @minLength 1 */
  type: string;
  /** @minLength 1 */
  reason: string;
  /** @format double */
  locationLatitude: number;
  /** @format double */
  locationLongitude: number;
  /** @format double */
  locationAltitude: number;
  /** @format date-time */
  estimatedTime: string;
}

/** DTO for ru.itmopai.backend.entity.CaseEntity */
export interface CaseDto {
  /** @format int64 */
  id?: number;
  /** @minLength 1 */
  title: string;
  description?: string;
  /** @format int64 */
  creatorId?: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId?: number;
  /** @format date-time */
  updatedAt: string;
  status: CaseEntityStatus;
  /** @format int64 */
  incidentId?: number;
  users: Record<string, string>;
}

/** DTO for ru.itmopai.backend.entity.CaseEntity */
export interface CaseMainInfoDto {
  /** @format int64 */
  id: number;
  /** @minLength 1 */
  title: string;
  description: string;
  status: CaseEntityStatus;
}

/** DTO for ru.itmopai.backend.entity.IncidentEntity */
export interface IncidentMainInfoDto {
  /** @format int64 */
  id: number;
  applicant: string;
  /** @minLength 1 */
  type: string;
  /** @minLength 1 */
  reason: string;
  /** @format double */
  locationLatitude: number;
  /** @format double */
  locationLongitude: number;
  /** @format double */
  locationAltitude: number;
  /** @format date-time */
  estimatedTime: string;
}

export interface LocationUpdateRequest {
  /** @format double */
  latitude: number;
  /** @format double */
  longitude: number;
  /** @format double */
  altitude: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  /** @format int32 */
  expiresIn: number;
  username: string;
  /** @format int64 */
  id: number;
  /** @format int64 */
  iat: number;
}

export interface RegisterRequest {
  /** @minLength 1 */
  username: string;
  /** @minLength 1 */
  password: string;
  /** @minLength 1 */
  name: string;
  /** @minLength 1 */
  surname: string;
  patronymic: string;
}

export interface UpdateCaseRequest {
  /** @minLength 1 */
  title: string;
  description: string;
  /** @minLength 1 */
  status: string;
  users: Record<string, string>;
}

export interface UserDto {
  /** @format int64 */
  id: number;
  /** @minLength 1 */
  username: string;
  /** @minLength 1 */
  name: string;
  /** @minLength 1 */
  surname: string;
  patronymic: string;
  /** @format double */
  locationLatitude: number;
  /** @format double */
  locationLongitude: number;
  /** @format double */
  locationAltitude: number;
}

export interface UserInfoResponse {
  /** @format int64 */
  id: number;
  username: string;
}

export interface UserPermissionsRequest {
  permissions: string[];
}

export interface UserPermissionsResponse {
  permissions: string[];
}
