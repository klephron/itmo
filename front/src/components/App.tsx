import { createStore } from "solid-js/store";
import { createSignal, Match, onMount, Switch } from "solid-js";
import { clientGet } from "@state/client";
import {
  CellItemDto,
  DriveDto,
  ExportBusDto,
  ImportBusDto,
  MeInterfaceDto,
  NetworkDto,
  PatternDto,
  StorageCellDto,
} from "@api/generated/data-contracts";
import styles from "./App.module.scss";
import { Section } from "./kind";
import { CellItemTable } from "./CellItemTable";
import { DriveTable } from "./DriveTable";
import { ExportBusTable } from "./ExportBusTable";
import { ImportBusTable } from "./ImportBusTable";
import { InterfaceTable } from "./InterfaceTable";
import { NetworkTable } from "./NetworkTable";
import { PatternTable } from "./PatternTable";
import { StorageCellTable } from "./StorageCellTable";
import { CellItemOperations } from "./CellItemOperations";
import { NetworkOperations } from "./NetworkOperations";
import { DriveOperations } from "./DriveOperations";
import { ExportBusOperations } from "./ExportBusOperations";
import { ImportBusOperations } from "./ImportBusOperations";
import { InterfaceOperations } from "./InterfaceOperations";
import { PatternOperations } from "./PatternOperations";
import { StorageCellOperations } from "./StorageCellOperations";

export const App = () => {
  const client = clientGet();
  const [sectionGet, sectionSet] = createSignal(Section.CellItems);

  const [cellItems, cellItemsSet] = createStore<CellItemDto[]>([]);
  const [drives, drivesSet] = createStore<DriveDto[]>([]);
  const [exportBuses, exportBusesSet] = createStore<ExportBusDto[]>([]);
  const [importBuses, importBusesSet] = createStore<ImportBusDto[]>([]);
  const [interfaces, interfacesSet] = createStore<MeInterfaceDto[]>([]);
  const [networks, networksSet] = createStore<NetworkDto[]>([]);
  const [patterns, patternsSet] = createStore<PatternDto[]>([]);
  const [storageCells, storageCellsSet] = createStore<StorageCellDto[]>([]);

  function fetchSection(section: Section) {
    switch (section) {
      case Section.CellItems:
        client.cellItems
          .getAllCellItems()
          .then((r) =>
            r.json().then((data: CellItemDto[]) => cellItemsSet(data)),
          )
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.Drives:
        client.drives
          .getAllDrives()
          .then((r) => r.json().then((data: DriveDto[]) => drivesSet(data)))
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.ExportBuses:
        client.exportBuses
          .getAllExportBuses()
          .then((r) =>
            r.json().then((data: ExportBusDto[]) => exportBusesSet(data)),
          )
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.ImportBuses:
        client.importBuses
          .getAllImportBuses()
          .then((r) =>
            r.json().then((data: ImportBusDto[]) => importBusesSet(data)),
          )
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.Interfaces:
        client.interfaces
          .getAllInterfaces()
          .then((r) =>
            r.json().then((data: MeInterfaceDto[]) => interfacesSet(data)),
          )
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.Networks:
        client.networks
          .getAllNetworks()
          .then((r) => r.json().then((data: NetworkDto[]) => networksSet(data)))
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.Patterns:
        client.patterns
          .getAllPatterns()
          .then((r) => r.json().then((data: PatternDto[]) => patternsSet(data)))
          .catch(async (e) => alert(await e.text()));
        break;
      case Section.StorageCells:
        client.storageCells
          .getAllStorageCells()
          .then((r) =>
            r.json().then((data: StorageCellDto[]) => storageCellsSet(data)),
          )
          .catch(async (e) => alert(await e.text()));
        break;
    }
  }

  onMount(() => {
    fetchSection(sectionGet());
  });

  return (
    <div class={styles.app}>
      <div class={styles.layout}>
        <nav class={styles.sidebar}>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.CellItems,
            }}
            onClick={() => {
              sectionSet(Section.CellItems);
              fetchSection(Section.CellItems);
            }}
          >
            Cell Items
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.Drives,
            }}
            onClick={() => {
              sectionSet(Section.Drives);
              fetchSection(Section.Drives);
            }}
          >
            Drives
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.ExportBuses,
            }}
            onClick={() => {
              sectionSet(Section.ExportBuses);
              fetchSection(Section.ExportBuses);
            }}
          >
            Export Buses
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.ImportBuses,
            }}
            onClick={() => {
              sectionSet(Section.ImportBuses);
              fetchSection(Section.ImportBuses);
            }}
          >
            Import Buses
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.Interfaces,
            }}
            onClick={() => {
              sectionSet(Section.Interfaces);
              fetchSection(Section.Interfaces);
            }}
          >
            ME Interfaces
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.Networks,
            }}
            onClick={() => {
              sectionSet(Section.Networks);
              fetchSection(Section.Networks);
            }}
          >
            Networks
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.Patterns,
            }}
            onClick={() => {
              sectionSet(Section.Patterns);
              fetchSection(Section.Patterns);
            }}
          >
            Patterns
          </button>
          <button
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.StorageCells,
            }}
            onClick={() => {
              sectionSet(Section.StorageCells);
              fetchSection(Section.StorageCells);
            }}
          >
            Storage Cells
          </button>
        </nav>
        <div class={styles.tableContent}>
          <Switch>
            <Match when={sectionGet() === Section.CellItems}>
              <CellItemTable items={cellItems} />
            </Match>
            <Match when={sectionGet() === Section.Drives}>
              <DriveTable items={drives} />
            </Match>
            <Match when={sectionGet() === Section.ExportBuses}>
              <ExportBusTable items={exportBuses} />
            </Match>
            <Match when={sectionGet() === Section.ImportBuses}>
              <ImportBusTable items={importBuses} />
            </Match>
            <Match when={sectionGet() === Section.Interfaces}>
              <InterfaceTable items={interfaces} />
            </Match>
            <Match when={sectionGet() === Section.Networks}>
              <NetworkTable items={networks} />
            </Match>
            <Match when={sectionGet() === Section.Patterns}>
              <PatternTable items={patterns} />
            </Match>
            <Match when={sectionGet() === Section.StorageCells}>
              <StorageCellTable items={storageCells} />
            </Match>
          </Switch>
        </div>
        <Switch>
          <Match when={sectionGet() === Section.CellItems}>
            <CellItemOperations
              onUpdate={() => fetchSection(Section.CellItems)}
            />
          </Match>
          <Match when={sectionGet() === Section.Drives}>
            <DriveOperations onUpdate={() => fetchSection(Section.Drives)} />
          </Match>
          <Match when={sectionGet() === Section.ExportBuses}>
            <ExportBusOperations
              onUpdate={() => fetchSection(Section.ExportBuses)}
              onExport={() => { }}
            />
          </Match>
          <Match when={sectionGet() === Section.ImportBuses}>
            <ImportBusOperations
              onUpdate={() => fetchSection(Section.ImportBuses)}
              onImport={() => { }}
            />
          </Match>
          <Match when={sectionGet() === Section.Interfaces}>
            <InterfaceOperations
              onUpdate={() => fetchSection(Section.Interfaces)}
            />
          </Match>
          <Match when={sectionGet() === Section.Networks}>
            <NetworkOperations
              onUpdate={() => fetchSection(Section.Networks)}
            />
          </Match>
          <Match when={sectionGet() === Section.Patterns}>
            <PatternOperations
              onUpdate={() => fetchSection(Section.Patterns)}
              onCraft={() => { }}
            />
          </Match>
          <Match when={sectionGet() === Section.StorageCells}>
            <StorageCellOperations
              onUpdate={() => fetchSection(Section.StorageCells)}
            />
          </Match>
        </Switch>
      </div>
    </div>
  );
};
