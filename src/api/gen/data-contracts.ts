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

export interface LocationUpdateRequest {
  /** @format double */
  latitude: number;
  /** @format double */
  longitude: number;
  /** @format double */
  altitude: number;
}

export interface RegisterRequest {
  username: string;
  password: string;
  name: string;
  surname: string;
  patronymic: string;
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
