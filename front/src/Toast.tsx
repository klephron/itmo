import { Portal } from "solid-js/web";

export const Toast = (props: any) => {
  return (
    <Portal>
      <div class="toast" role={props.role ?? "log"}>
        {props.children}
      </div>
    </Portal>
  );
};
