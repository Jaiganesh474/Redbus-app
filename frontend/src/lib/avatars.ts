export interface AvatarOption {
  id: string;
  name: string;
  gender: "MALE" | "FEMALE";
  url: string;
}

export const MALE_AVATARS: AvatarOption[] = [
  {
    id: "m1",
    name: "Alex (Explorer)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Alex&backgroundColor=b6e3f4",
  },
  {
    id: "m2",
    name: "Rohan (Tech Guy)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Rohan&backgroundColor=c0aede",
  },
  {
    id: "m3",
    name: "Karan (Casual)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Karan&backgroundColor=d1d4f9",
  },
  {
    id: "m4",
    name: "Vikram (Business)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/micah/svg?seed=Vikram&backgroundColor=ffd5dc",
  },
  {
    id: "m5",
    name: "Arjun (Traveler)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/personas/svg?seed=Arjun&backgroundColor=ffdfbf",
  },
  {
    id: "m6",
    name: "Sameer (Urban)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/avataaars/svg?seed=Sameer&backgroundColor=b6e3f4",
  },
];

export const FEMALE_AVATARS: AvatarOption[] = [
  {
    id: "f1",
    name: "Priya (Adventurer)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Priya&backgroundColor=ffd5dc",
  },
  {
    id: "f2",
    name: "Ananya (Casual)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Ananya&backgroundColor=ffdfbf",
  },
  {
    id: "f3",
    name: "Sneha (Tech Lead)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/personas/svg?seed=Sneha&backgroundColor=c0aede",
  },
  {
    id: "f4",
    name: "Riya (Urban)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/micah/svg?seed=Riya&backgroundColor=b6e3f4",
  },
  {
    id: "f5",
    name: "Meera (Classic)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/avataaars/svg?seed=Meera&backgroundColor=d1d4f9",
  },
  {
    id: "f6",
    name: "Tara (Creative)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Tara&backgroundColor=ffd5dc",
  },
];

export const ALL_AVATARS = [...MALE_AVATARS, ...FEMALE_AVATARS];

export function getDefaultAvatar(gender?: string, name?: string): string {
  const seed = name ? encodeURIComponent(name) : "Traveler";
  if (gender === "FEMALE") {
    return `https://api.dicebear.com/7.x/adventurer/svg?seed=${seed}&backgroundColor=ffd5dc`;
  }
  return `https://api.dicebear.com/7.x/adventurer/svg?seed=${seed}&backgroundColor=b6e3f4`;
}
