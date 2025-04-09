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

export interface StorageCellEntityDto {
  /** @format int64 */
  id?: number;
  /**
   * @format int32
   * @min 0
   */
  capacity: number;
  /** @format int64 */
  driveId: number;
}

export interface PatternEntityDto {
  /** @format int64 */
  id?: number;
  patternName: string;
  inputItems: string;
  /** @format int64 */
  outputItemId?: number;
  /**
   * @format int32
   * @min 0
   */
  outputQuantity: number;
  /** @format int64 */
  meInterfaceId?: number;
}

export interface NetworkDto {
  /** @format int64 */
  id?: number;
  name: string;
  /**
   * @format int32
   * @min 0
   */
  channelCapacity: number;
  /**
   * @format int32
   * @min 0
   */
  usedChannels: number;
}

export interface ItemDto {
  /** @format int64 */
  id?: number;
  itemName: string;
  /**
   * @format int32
   * @min 0
   */
  quantity: number;
}

export interface MeInterfaceDto {
  /** @format int64 */
  id?: number;
  interfaceName: string;
  /**
   * @format int32
   * @min 0
   */
  channelsUsed?: number;
  /** @format int64 */
  networkId: number;
}

export interface ImportBusEntityDto {
  /** @format int64 */
  id?: number;
  busName: string;
  /**
   * @format int32
   * @min 0
   */
  channelsUsed: number;
  /** @format int64 */
  networkId: number;
}

export interface ExportBusEntityDto {
  /** @format int64 */
  id?: number;
  busName: string;
  /**
   * @format int32
   * @min 0
   */
  channelsUsed?: number;
  /** @format int64 */
  networkId: number;
}

export interface DriveEntityDto {
  /** @format int64 */
  id?: number;
  driveName: string;
  /**
   * @format int32
   * @min 0
   */
  channelsUsed: number;
  /** @format int64 */
  networkId: number;
}

export interface CellItemEntityDto {
  /** @format int64 */
  id?: number;
  itemName: string;
  /**
   * @format int32
   * @min 0
   */
  quantity: number;
  /** @format int64 */
  storageCellId: number;
}
