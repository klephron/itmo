export default function Checkbox({
  isChecked,
  onChange,
  ...rest
}: {
  isChecked: boolean;
  onChange?: (e: boolean) => void;
}) {
  return (
    <input
      style={{ cursor: "pointer" }}
      type="checkbox"
      checked={isChecked}
      onChange={(e) => (onChange ? onChange(e.target.checked) : null)}
      {...rest}
    ></input>
  );
}
