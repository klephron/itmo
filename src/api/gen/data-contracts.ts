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

export interface AddCaseRequest {
  /** @minLength 1 */
  title: string;
  description: string;
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
}

export interface UserDto {
  /** @format int64 */
  id: number;
  /** @minLength 1 */
  username: string;
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

export interface UsernamePasswordCredentials {
  /** @minLength 1 */
  username: string | null;
  /** @minLength 1 */
  password: string | null;
  identity?: string | null;
  secret?: string | null;
}
