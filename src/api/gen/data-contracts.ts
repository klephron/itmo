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

export enum TemplateEntityStatus {
  PENDING = "PENDING",
  CREATED = "CREATED",
  CDB_ADDED = "CDB_ADDED",
}

export enum NotificationsControllerStatus {
  READ = "READ",
  UNREAD = "UNREAD",
  ALL = "ALL",
}

export enum ClueEntityStatus {
  ACCEPTED = "ACCEPTED",
  REJECTED = "REJECTED",
  PENDING = "PENDING",
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
  description?: string | null;
  users: Record<string, string>;
}

export interface AddClueRequest {
  /** @minLength 1 */
  title: string;
  /** @minLength 1 */
  description: string;
  status?: ClueEntityStatus | null;
}

export interface AddIncidentRequest {
  applicant?: string | null;
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

export interface AddTemplateRequest {
  /** @minLength 1 */
  title: string;
  /** @minLength 1 */
  metadata: string;
  description?: string | null;
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
  id?: number | null;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  status: CaseEntityStatus;
}

/** DTO for ru.itmopai.backend.entity.ClueEntity */
export interface ClueDto {
  /** @format int64 */
  id?: number | null;
  /** @format int64 */
  creatorId?: number | null;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId?: number | null;
  /** @format date-time */
  updatedAt: string;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  status: ClueEntityStatus;
  metadata?: string | null;
  /** @format int64 */
  relatedCaseId?: number | null;
}

/** DTO for ru.itmopai.backend.entity.ClueEntity */
export interface ClueMainInfoDto {
  /** @format int64 */
  id?: number | null;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  status: ClueEntityStatus;
}

/** DTO for ru.itmopai.backend.entity.IncidentEntity */
export interface IncidentMainInfoDto {
  /** @format int64 */
  id?: number | null;
  applicant?: string | null;
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
  latitude?: number | null;
  /** @format double */
  longitude?: number | null;
  /** @format double */
  altitude?: number | null;
}

export interface LoginRequest {
  /** @minLength 1 */
  username: string;
  /** @minLength 1 */
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

/** DTO for ru.itmopai.backend.entity.NotificationEntity */
export interface NotificationDto {
  /** @format int64 */
  id: number;
  /** @format int64 */
  userId: number;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  /** @format date-time */
  createdAt: string;
  /** @format date-time */
  readAt?: string | null;
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
  patronymic?: string | null;
}

/** DTO for ru.itmopai.backend.entity.TemplateEntity */
export interface TemplateDto {
  /** @format int64 */
  id?: number | null;
  /** @format int64 */
  creatorId?: number | null;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId?: number | null;
  /** @format date-time */
  updatedAt: string;
  /** @minLength 1 */
  title: string;
  status: TemplateEntityStatus;
  /** @minLength 1 */
  metadata: string;
  description?: string | null;
  uri?: string | null;
  cdbURI?: string | null;
  /** @format int64 */
  relatedCaseId?: number | null;
}

/** DTO for ru.itmopai.backend.entity.TemplateEntity */
export interface TemplateMainInfoDto {
  /** @format int64 */
  id?: number | null;
  /** @minLength 1 */
  title: string;
  status: TemplateEntityStatus;
  /** @minLength 1 */
  metadata: string;
  description?: string | null;
}

export interface UpdateCaseRequest {
  title?: string | null;
  description?: string | null;
  status?: CaseEntityStatus | null;
  users?: Record<string, string>;
}

export interface UpdateClueRequest {
  title?: string | null;
  description?: string | null;
  status?: ClueEntityStatus | null;
}

export interface UpdateIncidentRequest {
  applicant?: string | null;
  type?: string | null;
  reason?: string | null;
  /** @format double */
  locationLatitude?: number | null;
  /** @format double */
  locationLongitude?: number | null;
  /** @format double */
  locationAltitude?: number | null;
  /** @format date-time */
  estimatedTime?: string | null;
}

export interface UpdateNotificationRequest {
  title?: string | null;
  description?: string | null;
  /** @format date-time */
  readAt?: string | null;
}

export interface UpdateTemplateRequest {
  title?: string | null;
  status?: TemplateEntityStatus | null;
  metadata?: string | null;
  description?: string | null;
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
  patronymic?: string | null;
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
  permissions?: string[] | null;
}

export interface UserPermissionsResponse {
  permissions: string[];
}
