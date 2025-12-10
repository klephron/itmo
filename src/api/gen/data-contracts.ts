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

export enum UserCaseEntityStatus {
  WORKING = "WORKING",
  VACATION = "VACATION",
  TRANSFERED = "TRANSFERED",
}

export enum TemplateEntityStatus {
  PENDING = "PENDING",
  CREATED = "CREATED",
  CDB_ADDED = "CDB_ADDED",
}

export enum DefaultPermissions {
  OPERATOR = "OPERATOR",
  OFFICER = "OFFICER",
  ANDROID_INVESTIGATOR = "ANDROID_INVESTIGATOR",
  ANDROID_PATROLMAN = "ANDROID_PATROLMAN",
  ANDROID_OPERATIVE = "ANDROID_OPERATIVE",
  ADMIN = "ADMIN",
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

export enum CaseSuspectEntityStatus {
  SUSPECTED = "SUSPECTED",
  JUSTIFIED = "JUSTIFIED",
  ACCUSED = "ACCUSED",
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

export interface AddCaseSuspectRequest {
  name: string;
  surname: string;
  patronymic?: string | null;
  identifier: string;
  status: CaseSuspectEntityStatus;
  verdict?: string | null;
  accusation?: string | null;
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
  waitForAssignment: boolean;
}

export interface AddTemplateRequest {
  /** @minLength 1 */
  title: string;
  metadata: TemplateMetadata;
  description?: string | null;
}

export interface RankedUserInfoResponse {
  user: UserInfoResponse;
  score: number;
  distanceMeters: number;
  activeCases: number;
  positionMatches: boolean;
}

/** DTO for ru.itmopai.backend.entity.CaseEntity */
export interface CaseDto {
  /** @format int64 */
  id: number;
  /** @minLength 1 */
  title: string;
  description?: string;
  /** @format int64 */
  creatorId: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId: number;
  /** @format date-time */
  updatedAt: string;
  status: CaseEntityStatus;
  /** @format int64 */
  incidentId: number;
  users: Record<string, string>;
}

/** DTO for ru.itmopai.backend.entity.CaseSuspectEntity */
export interface CaseSuspectDto {
  /** @format int64 */
  id: number;
  /** @format int64 */
  creatorId: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId: number;
  /** @format date-time */
  updatedAt: string;
  name: string;
  surname: string;
  patronymic: string;
  identifier: string;
  status: CaseSuspectEntityStatus;
  /** @format int64 */
  relatedCaseId: number;
  verdict: string;
  accusation: string;
}

/** DTO for ru.itmopai.backend.entity.CaseSuspectEntity */
export interface CaseSuspectMainInfoDto {
  /** @format int64 */
  id: number;
  name: string;
  surname: string;
  patronymic: string;
  identifier: string;
  status: CaseSuspectEntityStatus;
  verdict: string;
  accusation: string;
}

/** DTO for ru.itmopai.backend.entity.ClueEntity */
export interface ClueDto {
  /** @format int64 */
  id: number;
  /** @format int64 */
  creatorId: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId: number;
  /** @format date-time */
  updatedAt: string;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  status: ClueEntityStatus;
  metadata?: ClueMetadata | null;
  /** @format int64 */
  relatedCaseId: number;
}

/** DTO for ru.itmopai.backend.entity.ClueEntity */
export interface ClueMainInfoDto {
  /** @format int64 */
  id: number;
  /** @minLength 1 */
  title: string;
  description?: string | null;
  status: ClueEntityStatus;
  metadata?: ClueMetadata | null;
}

/** DTO for ru.itmopai.backend.entity.IncidentEntity */
export interface IncidentDto {
  /** @format int64 */
  id: number;
  /** @format int64 */
  creatorId: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId: number;
  /** @format date-time */
  updatedAt: string;
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
  /** @format int64 */
  relatedCaseId?: number | null;
  waitForAssignment: boolean;
}

/** DTO for ru.itmopai.backend.entity.IncidentEntity */
export interface IncidentMainInfoDto {
  /** @format int64 */
  id: number;
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
  waitForAssignment: boolean;
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

export interface ClueLocation {
  latitude?: number | null;
  longitude?: number | null;
  altitude?: number | null;
}

export interface ClueMetadata {
  imageUris?: string[] | null;
  videoUris?: string[] | null;
  reportUris?: string[] | null;
  location?: ClueLocation | null;
}

export interface FileIdResponse {
  /** @format uuid */
  id: string;
}

export interface FileMetaResponse {
  /** @format uuid */
  id: string;
  filename: string;
  contentType: string;
  /** @format int64 */
  sizeBytes: number;
  downloadUrl: string;
  inlineUrl: string;
}

export interface LogEntryDto {
  /** @format int64 */
  id?: number;
  /** @format date-time */
  createdAt: string;
  level: string;
  loggerName: string;
  threadName: string;
  message: string;
  exception?: string | null;
}

export interface PageLogEntryDto {
  content: LogEntryDto[];
  totalSize?: number;
  totalPages?: number;
  pageNumber?: number;
  size?: number;
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
  primaryPosition?: string | null;
}

/** DTO for ru.itmopai.backend.entity.TemplateEntity */
export interface TemplateDto {
  /** @format int64 */
  id: number;
  /** @format int64 */
  creatorId: number;
  /** @format date-time */
  createdAt: string;
  /** @format int64 */
  updatorId: number;
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
  relatedCaseId: number;
}

/** DTO for ru.itmopai.backend.entity.TemplateEntity */
export interface TemplateMainInfoDto {
  /** @format int64 */
  id: number;
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

export interface UpdateCaseSuspectRequest {
  name?: string | null;
  surname?: string | null;
  patronymic?: string | null;
  identifier?: string | null;
  status?: CaseSuspectEntityStatus | null;
  verdict?: string | null;
  accusation?: string | null;
}

export interface UpdateClueRequest {
  title?: string | null;
  description?: string | null;
  status?: ClueEntityStatus | null;
  metadata: ClueMetadata;
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
  waitForAssignment: boolean;
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
  metadata?: TemplateMetadata | null;
  description?: string | null;
  uri?: string | null;
  cdbId?: string | null;
  cdbURI?: string | null;
}

export interface DefaultPermissionsRequest {
  preset: DefaultPermissions;
}

export interface PermissionsInScopeResponse {
  permissions: string[];
}

export interface ScopesResponse {
  scopes: string[];
}

export enum UserPrimaryPosition {
  OPERATOR = "OPERATOR",
  OFFICER = "OFFICER",
  INVESTIGATOR = "INVESTIGATOR",
}

export interface TemplateMetadata {
  clueIds?: number[];
  suspectIds?: number[];
  employeeIds?: number[];
  templateFileUrl?: string;
  templateFileId?: string;
  templateFileName?: string;
  templateContentType?: string;
  reportFileUrl?: string;
  reportFileId?: string;
  reportFileName?: string;
  reportContentType?: string;
}

/** DTO for ru.itmopai.backend.entity.UserCaseEntity */
export interface UserCaseDto {
  /** @format int64 */
  idUserId: number;
  /** @format int64 */
  idRelatedCaseId: number;
  idRelatedCaseTitle: string;
  /** @minLength 1 */
  position: string;
  status: UserCaseEntityStatus;
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
  primaryPosition?: string | null;
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
