import { createStore } from "solid-js/store";
import { createSignal, Match, onMount, Switch } from "solid-js";
import { clientGet } from "@state/client";
import {
  CellItemEntityDto,
  DriveEntityDto,
  ExportBusEntityDto,
  ImportBusEntityDto,
  ItemDto,
  MeInterfaceDto,
  NetworkDto,
  PatternEntityDto,
  StorageCellEntityDto,
} from "@api/generated/data-contracts";
import styles from "./App.module.scss";
import { Section } from "./kind";
import { CellItemTable } from "./CellItemTable";
import { DriveTable } from "./DriveTable";
import { ExportBusTable } from "./ExportBusTable";
import { ImportBusTable } from "./ImportBusTable";
import { InterfaceTable } from "./InterfaceTable";
import { ItemTable } from "./ItemTable";
import { NetworkTable } from "./NetworkTable";
import { PatternTable } from "./PatternTable";
import { StorageCellTable } from "./StorageCellTable";
import { CellItemOperations } from "./CellItemOperations";
import { NetworkOperations } from "./NetworkOperations";
import { DriveOperations } from "./DriveOperations";
import { ExportBusOperations } from "./ExportBusOperations";
import { ImportBusOperations } from "./ImportBusOperations";
import { InterfaceOperations } from "./InterfaceOperations";
import { ItemOperations } from "./ItemOperations";
import { PatternOperations } from "./PatternOperations";
import { StorageCellOperations } from "./StorageCellOperations";

export const App = () => {
  const client = clientGet();
  const [sectionGet, sectionSet] = createSignal(Section.CellItems);

  const [cellItems, cellItemsSet] = createStore<CellItemEntityDto[]>([]);
  const [drives, drivesSet] = createStore<DriveEntityDto[]>([]);
  const [exportBuses, exportBusesSet] = createStore<ExportBusEntityDto[]>([]);
  const [importBuses, importBusesSet] = createStore<ImportBusEntityDto[]>([]);
  const [interfaces, interfacesSet] = createStore<MeInterfaceDto[]>([]);
  const [items, itemsSet] = createStore<ItemDto[]>([]);
  const [networks, networksSet] = createStore<NetworkDto[]>([]);
  const [patterns, patternsSet] = createStore<PatternEntityDto[]>([]);
  const [storageCells, storageCellsSet] = createStore<StorageCellEntityDto[]>(
    [],
  );

  function fetchSection(section: Section) {
    switch (section) {
      case Section.CellItems:
        client.cellItems.getAllCellItems().then((r) => {
          r.json().then((data: CellItemEntityDto[]) => {
            cellItemsSet(data);
          });
        });
        break;
      case Section.Drives:
        client.drives.getAllDrives().then((r) => {
          r.json().then((data: DriveEntityDto[]) => {
            drivesSet(data);
          });
        });
        break;
      case Section.ExportBuses:
        client.exportBuses.getAllExportBuses().then((r) => {
          r.json().then((data: ExportBusEntityDto[]) => {
            exportBusesSet(data);
          });
        });
        break;
      case Section.ImportBuses:
        client.importBuses.getAllImportBuses().then((r) => {
          r.json().then((data: ImportBusEntityDto[]) => {
            importBusesSet(data);
          });
        });
        break;
      case Section.Interfaces:
        client.interfaces.getAllInterfaces().then((r) => {
          r.json().then((data: MeInterfaceDto[]) => {
            interfacesSet(data);
          });
        });
        break;
      case Section.Items:
        client.items.getAllItems().then((r) => {
          r.json().then((data: ItemDto[]) => {
            itemsSet(data);
          });
        });
        break;
      case Section.Networks:
        client.networks.getAllNetworks().then((r) => {
          r.json().then((data: NetworkDto[]) => {
            networksSet(data);
          });
        });
        break;
      case Section.Patterns:
        client.patterns.getAllPatterns().then((r) => {
          r.json().then((data: PatternEntityDto[]) => {
            patternsSet(data);
          });
        });
        break;
      case Section.StorageCells:
        client.storageCells.getAllStorageCells().then((r) => {
          r.json().then((data: StorageCellEntityDto[]) => {
            storageCellsSet(data);
          });
        });
        break;
      default:
        console.warn("unknown section " + section + " to update");
    }
  }

  onMount(() => {
    fetchSection(sectionGet());
  });

  return (
    <div class={styles.app}>
      <div class={styles.layout}>
        <nav class={styles.sidebar}>
          <div
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
          </div>
          <div
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
          </div>
          <div
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
          </div>
          <div
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
          </div>
          <div
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
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: sectionGet() === Section.Items,
            }}
            onClick={() => {
              sectionSet(Section.Items);
              fetchSection(Section.Items);
            }}
          >
            Items
          </div>
          <div
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
          </div>
          <div
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
          </div>
          <div
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
          </div>
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
            <Match when={sectionGet() === Section.Items}>
              <ItemTable items={items} />
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
              onExport={() => {}}
            />
          </Match>
          <Match when={sectionGet() === Section.ImportBuses}>
            <ImportBusOperations
              onUpdate={() => fetchSection(Section.ImportBuses)}
              onImport={() => {}}
            />
          </Match>
          <Match when={sectionGet() === Section.Interfaces}>
            <InterfaceOperations
              onUpdate={() => fetchSection(Section.Interfaces)}
            />
          </Match>
          <Match when={sectionGet() === Section.Items}>
            <ItemOperations onUpdate={() => fetchSection(Section.Items)} />
          </Match>
          <Match when={sectionGet() === Section.Networks}>
            <NetworkOperations
              onUpdate={() => fetchSection(Section.Networks)}
            />
          </Match>
          <Match when={sectionGet() === Section.Patterns}>
            <PatternOperations
              onUpdate={() => fetchSection(Section.Patterns)}
              onCraft={() => {}}
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
