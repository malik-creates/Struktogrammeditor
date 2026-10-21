import random

def zahlenraten(maximum=100):
    ziel = random.randint(1, maximum)
    versuche = 0
    while True:
        tipp = int(input("Dein Tipp: "))
        versuche += 1
        if tipp < ziel:
            print("Zu klein!")
        elif tipp > ziel:
            print("Zu gross!")
        else:
            print("Richtig!")
            break
    return versuche
