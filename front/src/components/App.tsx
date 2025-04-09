import { createStore } from "solid-js/store";
import { createSignal, onMount } from "solid-js";
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

enum Section {
  CellItems,
  Drives,
  ExportBuses,
  ImportBuses,
  Interfaces,
  Items,
  Networks,
  Patterns,
  StorageCells,
}

export const App = () => {
  const client = clientGet();
  const [selectedSection, selectedSectionSet] = createSignal(Section.CellItems);

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
          r.json().then((data) => {
            cellItemsSet(data);
          });
        });
        break;
      case Section.Drives:
        client.drives.getAllDrives().then((r) => {
          r.json().then((data) => {
            drivesSet(data);
          });
        });
        break;
      case Section.ExportBuses:
        client.exportBuses.getAllExportBuses().then((r) => {
          r.json().then((data) => {
            exportBusesSet(data);
          });
        });
        break;
      case Section.ImportBuses:
        client.importBuses.getAllImportBuses().then((r) => {
          r.json().then((data) => {
            importBusesSet(data);
          });
        });
        break;
      case Section.Interfaces:
        client.interfaces.getAllInterfaces().then((r) => {
          r.json().then((data) => {
            interfacesSet(data);
          });
        });
        break;
      case Section.Items:
        client.items.getAllItems().then((r) => {
          r.json().then((data) => {
            itemsSet(data);
          });
        });
        break;
      case Section.Networks:
        client.networks.getAllNetworks().then((r) => {
          r.json().then((data) => {
            networksSet(data);
          });
        });
        break;
      case Section.Patterns:
        client.patterns.getAllPatterns().then((r) => {
          r.json().then((data) => {
            patternsSet(data);
          });
        });
        break;
      case Section.StorageCells:
        client.storageCells.getAllStorageCells().then((r) => {
          r.json().then((data) => {
            storageCellsSet(data);
          });
        });
        break;
      default:
        console.warn("unknown section " + section + " to update");
    }
  }

  onMount(() => {
    fetchSection(selectedSection());
  });

  return (
    <div class={styles.app}>
      <div class={styles.layout}>
        <nav class={styles.sidebar}>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.CellItems,
            }}
            onClick={() => {
              selectedSectionSet(Section.CellItems);
              fetchSection(Section.CellItems);
            }}
          >
            Cell Items
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.Drives,
            }}
            onClick={() => {
              selectedSectionSet(Section.Drives);
              fetchSection(Section.Drives);
            }}
          >
            Drives
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.ExportBuses,
            }}
            onClick={() => {
              selectedSectionSet(Section.ExportBuses);
              fetchSection(Section.ExportBuses);
            }}
          >
            Export Buses
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.ImportBuses,
            }}
            onClick={() => {
              selectedSectionSet(Section.ImportBuses);
              fetchSection(Section.ImportBuses);
            }}
          >
            Import Buses
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.Interfaces,
            }}
            onClick={() => {
              selectedSectionSet(Section.Interfaces);
              fetchSection(Section.Interfaces);
            }}
          >
            Interfaces
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.Items,
            }}
            onClick={() => {
              selectedSectionSet(Section.Items);
              fetchSection(Section.Items);
            }}
          >
            Items
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.Networks,
            }}
            onClick={() => {
              selectedSectionSet(Section.Networks);
              fetchSection(Section.Networks);
            }}
          >
            Networks
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.Patterns,
            }}
            onClick={() => {
              selectedSectionSet(Section.Patterns);
              fetchSection(Section.Patterns);
            }}
          >
            Patterns
          </div>
          <div
            class={styles.navItem}
            classList={{
              [styles.active]: selectedSection() === Section.StorageCells,
            }}
            onClick={() => {
              selectedSectionSet(Section.StorageCells);
              fetchSection(Section.StorageCells);
            }}
          >
            Storage Cells
          </div>
        </nav>

        <main class={styles.content}>
          {selectedSection() === Section.CellItems && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Item Name</th>
                  <th>Quantity</th>
                  <th>Storage Cell ID</th>
                </tr>
              </thead>
              <tbody>
                {cellItems.map((item) => (
                  <tr>
                    <td>{item.id}</td>
                    <td>{item.itemName}</td>
                    <td>{item.quantity}</td>
                    <td>{item.storageCellId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.Drives && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Drive Name</th>
                  <th>Channels Used</th>
                  <th>Network ID</th>
                </tr>
              </thead>
              <tbody>
                {drives.map((drive) => (
                  <tr>
                    <td>{drive.id}</td>
                    <td>{drive.driveName}</td>
                    <td>{drive.channelsUsed}</td>
                    <td>{drive.networkId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.ExportBuses && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Bus Name</th>
                  <th>Channels Used</th>
                  <th>Network ID</th>
                </tr>
              </thead>
              <tbody>
                {exportBuses.map((bus) => (
                  <tr>
                    <td>{bus.id}</td>
                    <td>{bus.busName}</td>
                    <td>{bus.channelsUsed}</td>
                    <td>{bus.networkId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.ImportBuses && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Bus Name</th>
                  <th>Channels Used</th>
                  <th>Network ID</th>
                </tr>
              </thead>
              <tbody>
                {importBuses.map((bus) => (
                  <tr>
                    <td>{bus.id}</td>
                    <td>{bus.busName}</td>
                    <td>{bus.channelsUsed}</td>
                    <td>{bus.networkId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.Interfaces && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Interface Name</th>
                  <th>Channels Used</th>
                  <th>Network Id</th>
                </tr>
              </thead>
              <tbody>
                {interfaces.map((item) => (
                  <tr>
                    <td>{item.id}</td>
                    <td>{item.interfaceName}</td>
                    <td>{item.channelsUsed}</td>
                    <td>{item.networkId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.Items && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Item Name</th>
                  <th>Quantity</th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr>
                    <td>{item.id}</td>
                    <td>{item.itemName}</td>
                    <td>{item.quantity}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.Networks && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Name</th>
                  <th>Channel Capacity</th>
                  <th>Used Channels</th>
                </tr>
              </thead>
              <tbody>
                {networks.map((network) => (
                  <tr>
                    <td>{network.id}</td>
                    <td>{network.name}</td>
                    <td>{network.channelCapacity}</td>
                    <td>{network.usedChannels}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.Patterns && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Pattern Name</th>
                  <th>Input Items</th>
                  <th>Output Item ID</th>
                  <th>Output Quantity</th>
                  <th>ME Interface ID</th>
                </tr>
              </thead>
              <tbody>
                {patterns.map((pattern) => (
                  <tr>
                    <td>{pattern.id}</td>
                    <td>{pattern.patternName}</td>
                    <td>{pattern.inputItems}</td>
                    <td>{pattern.outputItemId}</td>
                    <td>{pattern.outputQuantity}</td>
                    <td>{pattern.meInterfaceId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {selectedSection() === Section.StorageCells && (
            <table class={styles.table}>
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Capacity</th>
                  <th>Drive ID</th>
                </tr>
              </thead>
              <tbody>
                {storageCells.map((cell) => (
                  <tr>
                    <td>{cell.id}</td>
                    <td>{cell.capacity}</td>
                    <td>{cell.driveId}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </main>
      </div>
    </div>
  );
};
