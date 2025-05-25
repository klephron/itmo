import { useUser } from "@state/user";

export default function IndexPage() {
  const user = useUser((state) => state.user);
  return (
    <>
      <p>
        Index page for user: {user.username} with permissions: {JSON.stringify(user.permissions)}
      </p>
    </>
  );
}
