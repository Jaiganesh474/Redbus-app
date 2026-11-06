export interface AvatarOption {
  id: string;
  name: string;
  gender: "MALE" | "FEMALE";
  url: string;
}

export const MALE_AVATARS: AvatarOption[] = [
  {
    id: "m-3d",
    name: "Male 3D",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Alex&backgroundColor=b6e3f4",
  },
];

export const FEMALE_AVATARS: AvatarOption[] = [
  {
    id: "f-3d",
    name: "Female 3D",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Priya&backgroundColor=ffd5dc",
  },
];

export const ALL_AVATARS = [...MALE_AVATARS, ...FEMALE_AVATARS];

export function getDefaultAvatar(gender?: string, name?: string): string {
  if (gender === "FEMALE") {
    return "https://api.dicebear.com/7.x/adventurer/svg?seed=Priya&backgroundColor=ffd5dc";
  }
  return "https://api.dicebear.com/7.x/adventurer/svg?seed=Alex&backgroundColor=b6e3f4";
}
