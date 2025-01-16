import { clientGet } from '@state/client';
import { subscriptionGet, subscriptionSet } from '@state/subscription';
import { setsEqual } from '@util/util';
import { ActorsAddMessage } from '@websocket/message';
import {
  ActorPack,
  ActorPackKind,
  IndividualActorPack,
  OrganizationActorPack,
  SendMailOneTaskPack,
  SendMailRepeatedTaskPack,
  TaskPack,
  TaskPackKind,
} from '@websocket/pack';
import { createEffect, createSignal, For, Match, Show, Switch } from 'solid-js';
import styles from './ViewActor.module.scss';
import { world } from '@state/world';
import MailTable from './MailTable';
import TaskTable from './TaskTable';
import { ActorAddTaskMessage } from '@websocket/message/actor-add-task';

export default function ViewActor() {
  const [getAddKind, setAddKind] = createSignal(ActorPackKind.INDIVIDUAL);
  const [getAddCountry, setAddCountry] = createSignal('');
  const [getAddCity, setAddCity] = createSignal('');
  const [getAddPostalCode, setAddPostalCode] = createSignal('');
  const [getAddStreet, setAddStreet] = createSignal('');
  const [getAddHouse, setAddHouse] = createSignal('');
  const [getAddName, setAddName] = createSignal('');
  const [getAddSurname, setAddSurname] = createSignal('');
  const [getAddPatronymic, setAddPatronymic] = createSignal('');
  const [getAddMailBufferCapacity, setAddMailBufferCapacity] = createSignal('');

  const [getSelectedIds, setSelectedIds] = createSignal(initSelected());

  createEffect(() => {
    updateSubscription();
  });

  function initSelected() {
    const subscription = subscriptionGet();
    if (subscription == null || subscription.args == null || subscription.args.actor_ids == null) {
      return new Set<number>();
    }
    return new Set(subscription.args.actor_ids);
  }

  function updateSubscription() {
    const subscription = subscriptionGet();
    if (subscription == null) {
      return;
    }
    const selectedIds = getSelectedIds();
    if (!setsEqual(new Set(subscription.args.actor_ids), selectedIds)) {
      subscription.args.actor_ids = [...selectedIds];
      subscriptionSet({ ...subscription });
    }
  }

  function addSubmit() {
    const client = clientGet();
    if (client == null) {
      return;
    }

    const data: ActorsAddMessage = {
      args: {
        actor: {
          kind: getAddKind(),
          address: {
            country: getAddCountry(),
            city: getAddCity(),
            postal_code: getAddPostalCode(),
            street: getAddStreet(),
            house: getAddHouse(),
          },
          name: getAddName(),
          surname: getAddSurname(),
          patronymic: getAddPatronymic(),
          mails_buffer_capacity: Number(getAddMailBufferCapacity()),
        },
      },
    };
    client.actorsAdd(data);
  }

  function addClear() {
    setAddCountry('');
    setAddCity('');
    setAddPostalCode('');
    setAddStreet('');
    setAddHouse('');
    setAddName('');
    setAddSurname('');
    setAddPatronymic('');
    setAddMailBufferCapacity('');
  }

  function toggleEntrySelection(e: ActorPack) {
    return () => {
      const selected = getSelectedIds();
      if (selected.has(e.id!)) {
        selected.delete(e.id!);
      } else {
        selected.add(e.id!);
      }
      setSelectedIds(new Set(selected));
    };
  }

  function getEntryExtraClass(e: ActorPack) {
    const selected = getSelectedIds();
    if (selected.has(e.id!)) {
      return styles.actors_table_body_row_extra + ' ' + styles.visible;
    } else {
      return styles.actors_table_body_row_extra;
    }
  }

  function TableEntryAddTask({ item }: { item: ActorPack }) {
    const [getTaskKind, setTaskKind] = createSignal(TaskPackKind.SEND_MAIL_ONE);
    const [getMailReceiverId, setMailReceiverId] = createSignal(
      world.actors == null || world.actors.length == 0 ? 0 : world.actors[0].id
    );
    const [getPostOfficeId, setPostOfficeId] = createSignal(
      world.postoffices == null || world.postoffices.length == 0 ? 0 : world.postoffices[0].id
    );
    const [getMailSubject, setMailSubject] = createSignal('');
    const [getMailText, setMailText] = createSignal('');

    const [getMailInterval, setMailInterval] = createSignal('');

    function addTaskSubmit() {
      const client = clientGet();
      if (client == null) {
        return;
      }
      const data = {
        args: {
          task: {
            kind: getTaskKind(),
            actor_id: Number(item.id),
            postoffice_id: Number(getPostOfficeId()),
          },
        },
      } as ActorAddTaskMessage;

      switch (getTaskKind()) {
        case TaskPackKind.SEND_MAIL_ONE: {
          (data.args.task as SendMailOneTaskPack).mail = {
            subject: getMailSubject(),
            text: getMailText(),
            sender_actor_id: item.id!,
            receiver_actor_id: Number(getMailReceiverId()),
          };
          break;
        }
        case TaskPackKind.SEND_MAIL_REPEATED: {
          (data.args.task as SendMailRepeatedTaskPack).mail = {
            subject: getMailSubject(),
            text: getMailText(),
            sender_actor_id: item.id!,
            receiver_actor_id: Number(getMailReceiverId()),
          };
          (data.args.task as SendMailRepeatedTaskPack).interval_ms = Number(getMailInterval());
          break;
        }
        case TaskPackKind.RECEIVE_MAIL_ONE: {
          break;
        }
        case TaskPackKind.RECEIVE_MAIL_MANY: {
          break;
        }
        case TaskPackKind.SEND_MARK_REPEATED: {
          break;
        }
      }
      client.actorAddTask(data);
    }

    function addTaskClear() {
      setTaskKind(TaskPackKind.SEND_MAIL_ONE);
      setMailReceiverId(world.actors == null || world.actors.length == 0 ? 0 : world.actors[0].id);
      setPostOfficeId(
        world.postoffices == null || world.postoffices.length == 0 ? 0 : world.postoffices[0].id
      );
      setMailSubject('');
      setMailText('');
    }

    return (
      <div class={styles.add_task}>
        <h4>Add Task</h4>
        <div class={styles.add_task_body}>
          <div>
            <span>Task Kind</span>
            <select
              value={getTaskKind()}
              onChange={(e) => {
                setTaskKind(e.target.value);
              }}
            >
              <For each={Object.values(TaskPackKind)}>
                {(item, _) => <option value={item}>{item}</option>}
              </For>
            </select>
          </div>
          <div>
            <span>Receiver Id</span>
            <select
              value={getMailReceiverId()}
              onChange={(e) => {
                setMailReceiverId(Number(e.target.value));
              }}
            >
              <For each={world.actors == null ? [] : world.actors}>
                {(item, _) => <option value={item.id}>{item.id}</option>}
              </For>
            </select>
          </div>
          <div>
            <span>Postoffice Id</span>
            <select
              value={getPostOfficeId()}
              onChange={(e) => {
                setPostOfficeId(Number(e.target.value));
              }}
            >
              <For each={world.postoffices == null ? [] : world.postoffices}>
                {(item, _) => <option value={item.id}>{item.id}</option>}
              </For>
            </select>
          </div>
          <Show when={getTaskKind() == TaskPackKind.SEND_MAIL_REPEATED}>
            <div>
              <span>Interval ms</span>
              <input
                value={getMailInterval()}
                onChange={(e) => setMailInterval(e.target.value)}
                placeholder="Interval ms"
              />
            </div>
          </Show>
          <Show
            when={
              getTaskKind() == TaskPackKind.SEND_MAIL_ONE ||
              getTaskKind() == TaskPackKind.SEND_MAIL_REPEATED
            }
          >
            <>
              <div>
                <span>Mail Subject</span>
                <input
                  value={getMailSubject()}
                  onChange={(e) => setMailSubject(e.target.value)}
                  placeholder="Mail Subject"
                />
              </div>
              <div>
                <span>Mail Text</span>
                <input
                  value={getMailText()}
                  onChange={(e) => setMailText(e.target.value)}
                  placeholder="Mail Text"
                />
              </div>
            </>
          </Show>
        </div>
        <div class={styles.add_task_buttons}>
          <button onClick={addTaskSubmit}>Submit</button>
          <button onClick={addTaskClear}>Clear</button>
        </div>
      </div>
    );
  }

  function TableEntry(item: ActorPack) {
    return (
      <>
        <div class={styles.actors_table_body_row} onClick={toggleEntrySelection(item)}>
          <div>{item.id}</div>
          <div>{item.kind}</div>
          <div>{item.mails_count}</div>
          <div>{item.mails_buffer_count}</div>
        </div>
        <div class={getEntryExtraClass(item)}>
          <div class={styles.operations}>
            <h4>Operations</h4>
            <div class={styles.operations_entries}>
              <TableEntryAddTask item={item} />
            </div>
          </div>
          <div>
            <h4>Parameters</h4>
            <div class={styles.parameters}>
              <div>
                <span>Buffer capacity</span>
                <input value={item.mails_buffer_capacity}></input>
              </div>
            </div>
          </div>
          <div>
            <h4>Personal</h4>
            <div class={styles.personal}>
              <Switch>
                <Match when={item.kind == ActorPackKind.INDIVIDUAL}>
                  <div>
                    <span>Name</span>
                    <input value={(item as IndividualActorPack).name}></input>
                  </div>
                  <div>
                    <span>Surname</span>
                    <input value={(item as IndividualActorPack).surname}></input>
                  </div>
                  <div>
                    <span>Patronymic</span>
                    <input value={(item as IndividualActorPack).patronymic}></input>
                  </div>
                </Match>
                <Match when={item.kind == ActorPackKind.ORGANIZATION}>
                  <div>
                    <span>Name</span>
                    <input value={(item as OrganizationActorPack).name}></input>
                  </div>
                </Match>
              </Switch>
            </div>
          </div>
          <div>
            <h4>Address</h4>
            {item.address == null ? (
              <div>Loading...</div>
            ) : (
              <div class={styles.address}>
                <div>
                  <span>Country</span>
                  <input value={item.address.country}></input>
                </div>
                <div>
                  <span>City</span>
                  <input value={item.address.city}></input>
                </div>
                <div>
                  <span>Postal Code</span>
                  <input value={item.address.postal_code}></input>
                </div>
                <div>
                  <span>Street</span>
                  <input value={item.address.street}></input>
                </div>
                <div>
                  <span>House</span>
                  <input value={item.address.house}></input>
                </div>
              </div>
            )}
          </div>
          <div>
            <h4>Tasks</h4>
            {item.tasks == null ? <div>Loading...</div> : <TaskTable data={item.tasks} />}
          </div>
          <div>
            <h4>Buffered Mails</h4>
            {item.mails_buffer == null ? (
              <div>Loading...</div>
            ) : (
              <MailTable data={item.mails_buffer} />
            )}
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <div class={styles.actors}>
        <div class={styles.actors_table}>
          <div class={styles.actors_table_header}>
            <div>ID</div>
            <div>Kind</div>
            <div>Mails Count</div>
            <div>Mails Buffered Count</div>
          </div>
          <div class={styles.actors_table_body}>
            <For each={world.actors} fallback={<div>Loading...</div>}>
              {(item, _) => TableEntry(item)}
            </For>
          </div>
        </div>
        <div class={styles.actors_operations}>
          <h4>Operations</h4>
          <div class={styles.add}>
            <h4>Add Actor</h4>
            <div>
              <input
                value={getAddMailBufferCapacity()}
                onChange={(e) => setAddMailBufferCapacity(e.target.value)}
                placeholder="Mail Buffer Capacity"
              />
            </div>
            <div>
              <select value={getAddKind()} onChange={(e) => setAddKind(e.target.value)}>
                <option value={ActorPackKind.INDIVIDUAL}>{ActorPackKind.INDIVIDUAL}</option>
                <option value={ActorPackKind.ORGANIZATION}>{ActorPackKind.ORGANIZATION}</option>
              </select>
            </div>
            <div>
              <input
                value={getAddCountry()}
                onChange={(e) => setAddCountry(e.target.value)}
                placeholder="Country"
              />
            </div>
            <div>
              <input
                value={getAddCity()}
                onChange={(e) => setAddCity(e.target.value)}
                placeholder="City"
              />
            </div>
            <div>
              <input
                value={getAddPostalCode()}
                onChange={(e) => setAddPostalCode(e.target.value)}
                placeholder="Postal Code"
              />
            </div>
            <div>
              <input
                value={getAddName()}
                onChange={(e) => setAddName(e.target.value)}
                placeholder="Name"
              />
            </div>
            <Show when={getAddKind() == ActorPackKind.INDIVIDUAL}>
              <div>
                <input
                  value={getAddSurname()}
                  onChange={(e) => setAddSurname(e.target.value)}
                  placeholder="Surname"
                />
              </div>
            </Show>
            <Show when={getAddKind() == ActorPackKind.INDIVIDUAL}>
              <div>
                <input
                  value={getAddPatronymic()}
                  onChange={(e) => setAddPatronymic(e.target.value)}
                  placeholder="Patronymic"
                />
              </div>
            </Show>
            <button onClick={addSubmit}>Submit</button>
            <button onClick={addClear}>Clear</button>
          </div>
        </div>
      </div>
    </>
  );
}
