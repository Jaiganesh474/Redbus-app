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
    url: "https://api.dicebear.com/7.x/bottts-neutral/svg?seed=Alex&mouth=smile01,smile02&eyes=roundFrame01,frame01&primaryColorLevel=600",
  },
  {
    id: "m2",
    name: "Rohan (Tech Guy)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/notionists/svg?seed=Rohan&gesture=wave&hair=short01,short02",
  },
  {
    id: "m3",
    name: "Karan (Casual)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Karan&hair=short01,short04&skinColor=f2d3b1",
  },
  {
    id: "m4",
    name: "Vikram (Business)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/micah/svg?seed=Vikram&hair=fonze,mrClean&baseColor=f9c9b6",
  },
  {
    id: "m5",
    name: "Arjun (Traveler)",
    gender: "MALE",
    url: "https://api.dicebear.com/7.x/lorelei/svg?seed=Arjun&hair=short02,short03",
  },
];

export const FEMALE_AVATARS: AvatarOption[] = [
  {
    id: "f1",
    name: "Priya (Adventurer)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/adventurer/svg?seed=Priya&hair=long01,long02&skinColor=f2d3b1",
  },
  {
    id: "f2",
    name: "Ananya (Casual)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/lorelei/svg?seed=Ananya&hair=wave,straight",
  },
  {
    id: "f3",
    name: "Sneha (Tech Lead)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/notionists/svg?seed=Sneha&gesture=point&hair=long01",
  },
  {
    id: "f4",
    name: "Riya (Urban)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/micah/svg?seed=Riya&hair=full,pixie&baseColor=f9c9b6",
  },
  {
    id: "f5",
    name: "Meera (Classic)",
    gender: "FEMALE",
    url: "https://api.dicebear.com/7.x/bottts-neutral/svg?seed=Meera&mouth=smile01&primaryColorLevel=600",
  },
];

export const ALL_AVATARS = [...MALE_AVATARS, ...FEMALE_AVATARS];

export function getDefaultAvatar(gender?: string, name?: string): string {
  if (gender === "FEMALE") {
    return FEMALE_AVATARS[0].url;
  }
  return MALE_AVATARS[0].url;
}
